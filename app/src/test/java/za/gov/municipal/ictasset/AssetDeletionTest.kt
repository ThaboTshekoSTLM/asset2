package za.gov.municipal.ictasset

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.test.*
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*
import za.gov.municipal.ictasset.data.local.LocalDataSeeder
import za.gov.municipal.ictasset.data.local.dao.ReferenceDao
import za.gov.municipal.ictasset.data.remote.SupabaseApi
import za.gov.municipal.ictasset.data.repository.SupabaseAssetRepository
import za.gov.municipal.ictasset.domain.model.*
import za.gov.municipal.ictasset.domain.repository.AssetRepository
import za.gov.municipal.ictasset.presentation.search.AssetHistoryViewModel

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AssetDeletionTest {
    private val admin = User(1, "Test admin", "admin", UserRole.ADMIN, true)

    private suspend fun fixture(): Pair<SupabaseApi, SupabaseAssetRepository> {
        val api = mock(SupabaseApi::class.java)
        val references = mock(ReferenceDao::class.java)
        `when`(references.allDepartments()).thenReturn(emptyList())
        `when`(references.allBuildings()).thenReturn(emptyList())
        `when`(references.allRooms()).thenReturn(emptyList())
        `when`(api.fetchAssets()).thenReturn(JSONArray("""[
          {"id":"asset-one","asset_barcode":"ONE"},
          {"id":"asset-two","asset_barcode":"TWO"}
        ]"""))
        `when`(api.fetchMovements()).thenReturn(JSONArray("""[
          {"id":"movement-one","asset_id":"asset-one"}
        ]"""))
        val repository = SupabaseAssetRepository(api, references, mock(LocalDataSeeder::class.java))
        repository.refresh()
        clearInvocations(api)
        return api to repository
    }

    @Test fun adminDeletionRemovesOnlySelectedAssetAndRetainsHistory() = runTest {
        val (api, repository) = fixture()
        val id = repository.currentAssets().first().id
        val history = repository.currentMovements()
        assertEquals(SaveResult.Success(id), repository.archiveAsset(id, admin))
        verify(api).archiveAsset("asset-one")
        assertEquals(listOf("TWO"), repository.currentAssets().map { it.assetBarcode })
        assertEquals(history, repository.currentMovements())
        assertEquals(SaveResult.Error("Asset not found."), repository.archiveAsset(id, admin))
        verifyNoMoreInteractions(api)
    }

    @Test fun everyNonAdminRoleIsRejectedWithoutNetworkChanges() = runTest {
        val (api, repository) = fixture()
        val id = repository.currentAssets().first().id
        UserRole.entries.filter { it != UserRole.ADMIN }.forEach { role ->
            assertEquals(SaveResult.Error("Admin access required."), repository.archiveAsset(id, admin.copy(role = role)))
        }
        assertEquals(2, repository.currentAssets().size)
        verifyNoInteractions(api)
    }

    @Test fun serverFailureKeepsAssetAndHistoryForRetry() = runTest {
        val (api, repository) = fixture()
        val before = repository.currentAssets()
        val history = repository.currentMovements()
        doThrow(IllegalStateException("Permission denied")).`when`(api).archiveAsset("asset-one")
        assertEquals(SaveResult.Error("Permission denied"), repository.archiveAsset(before.first().id, admin))
        assertEquals(before, repository.currentAssets())
        assertEquals(history, repository.currentMovements())
    }

    @Test fun cancellationDoesNotRemoveAsset() = runTest {
        val (api, repository) = fixture()
        val before = repository.currentAssets()
        doThrow(CancellationException("Cancelled")).`when`(api).archiveAsset("asset-one")
        try {
            repository.archiveAsset(before.first().id, admin)
            fail("Cancellation must propagate")
        } catch (_: CancellationException) {
            assertEquals(before, repository.currentAssets())
        }
    }

    @Test fun viewModelPreventsDuplicateRequestsAndAllowsRetryAfterFailure() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = mock(AssetRepository::class.java)
            `when`(repository.archiveAsset(42, admin))
                .thenReturn(SaveResult.Error("Offline"), SaveResult.Success(42))
            val model = AssetHistoryViewModel(repository)
            model.load(42)
            model.deleteAsset(admin)
            model.deleteAsset(admin)
            assertTrue(model.deleting.value)
            runCurrent()
            verify(repository, times(1)).archiveAsset(42, admin)
            assertFalse(model.deleting.value)
            assertFalse(model.deleted.value)
            assertEquals("Offline", model.deleteError.value)
            model.deleteAsset(admin)
            runCurrent()
            assertTrue(model.deleted.value)
            assertNull(model.deleteError.value)
            model.deleteAsset(admin)
            runCurrent()
            verify(repository, times(2)).archiveAsset(42, admin)
            model.viewModelScope.cancel()
            runCurrent()
        } finally {
            Dispatchers.resetMain()
        }
    }
}
