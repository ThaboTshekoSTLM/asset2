package za.gov.municipal.ictasset.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import za.gov.municipal.ictasset.domain.model.Asset
import za.gov.municipal.ictasset.domain.model.AssetMovement
import za.gov.municipal.ictasset.domain.repository.AssetRepository

class DashboardStatusViewModel(
    assetRepository: AssetRepository
) : ViewModel() {
    val assets: StateFlow<List<Asset>> =
        assetRepository.searchAssets("")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val movements: StateFlow<List<AssetMovement>> =
        assetRepository.observeRecentMovements(10_000)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
