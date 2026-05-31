package za.gov.municipal.ictasset.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import za.gov.municipal.ictasset.domain.model.Asset
import za.gov.municipal.ictasset.domain.model.AssetMovement
import za.gov.municipal.ictasset.domain.repository.AssetRepository

@OptIn(ExperimentalCoroutinesApi::class)
class AssetHistoryViewModel(
    private val assetRepository: AssetRepository
) : ViewModel() {
    private val selectedAssetId = MutableStateFlow<Long?>(null)

    val asset: StateFlow<Asset?> =
        selectedAssetId.flatMapLatest { assetId ->
            if (assetId == null) flowOf<Asset?>(null) else flow { emit(assetRepository.findAssetById(assetId)) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val movements: StateFlow<List<AssetMovement>> =
        selectedAssetId.flatMapLatest { assetId ->
            if (assetId == null) flowOf<List<AssetMovement>>(emptyList()) else assetRepository.observeMovementHistory(assetId)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun load(assetId: Long) {
        selectedAssetId.value = assetId
    }
}
