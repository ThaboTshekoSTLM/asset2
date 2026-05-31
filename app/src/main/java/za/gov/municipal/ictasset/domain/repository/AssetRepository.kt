package za.gov.municipal.ictasset.domain.repository

import kotlinx.coroutines.flow.Flow
import za.gov.municipal.ictasset.domain.model.Asset
import za.gov.municipal.ictasset.domain.model.AssetMovement
import za.gov.municipal.ictasset.domain.model.DashboardSummary
import za.gov.municipal.ictasset.domain.model.MoveAssetRequest
import za.gov.municipal.ictasset.domain.model.RegisterAssetRequest
import za.gov.municipal.ictasset.domain.model.SaveResult
import za.gov.municipal.ictasset.domain.model.User

interface AssetRepository {
    suspend fun seedIfNeeded()
    fun observeDashboardSummary(): Flow<DashboardSummary>
    fun observeRecentMovements(limit: Int = 10): Flow<List<AssetMovement>>
    fun searchAssets(query: String): Flow<List<Asset>>
    fun observeMovementHistory(assetId: Long): Flow<List<AssetMovement>>
    fun observeMovementHistoryByBarcode(assetBarcode: String): Flow<List<AssetMovement>>
    suspend fun findAssetByBarcode(barcode: String): Asset?
    suspend fun findAssetById(id: Long): Asset?
    suspend fun registerAsset(request: RegisterAssetRequest, actor: User): SaveResult
    suspend fun moveAsset(request: MoveAssetRequest, actor: User): SaveResult
}
