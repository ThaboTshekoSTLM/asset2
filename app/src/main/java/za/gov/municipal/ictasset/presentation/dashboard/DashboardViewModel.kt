package za.gov.municipal.ictasset.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import za.gov.municipal.ictasset.domain.model.AssetMovement
import za.gov.municipal.ictasset.domain.model.DashboardSummary
import za.gov.municipal.ictasset.domain.repository.AssetRepository

class DashboardViewModel(
    private val assetRepository: AssetRepository
) : ViewModel() {
    val summary: StateFlow<DashboardSummary> =
        assetRepository.observeDashboardSummary()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardSummary())

    val recentMovements: StateFlow<List<AssetMovement>> =
        assetRepository.observeRecentMovements(8)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
