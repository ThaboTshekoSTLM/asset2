package za.gov.municipal.ictasset.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import za.gov.municipal.ictasset.domain.model.Asset
import za.gov.municipal.ictasset.domain.repository.AssetRepository

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val assetRepository: AssetRepository
) : ViewModel() {
    val query = MutableStateFlow("")

    val assets: StateFlow<List<Asset>> =
        query.flatMapLatest { assetRepository.searchAssets(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateQuery(value: String) {
        query.value = value
    }
}
