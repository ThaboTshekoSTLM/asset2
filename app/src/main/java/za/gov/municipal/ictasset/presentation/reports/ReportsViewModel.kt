package za.gov.municipal.ictasset.presentation.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import za.gov.municipal.ictasset.domain.model.ExportedReport
import za.gov.municipal.ictasset.domain.model.ExportFormat
import za.gov.municipal.ictasset.domain.model.TabularReport
import za.gov.municipal.ictasset.domain.model.User
import za.gov.municipal.ictasset.domain.repository.ReportRepository
import za.gov.municipal.ictasset.domain.usecase.BuildReportsUseCase

enum class ReportScreenType(val label: String) {
    ASSETS_PER_DEPARTMENT("Assets per department"),
    ASSETS_PER_BUILDING("Assets per building"),
    MOVEMENT_HISTORY("Movement history"),
    ALLOCATED_TO_USER("Allocated to user"),
    MOVED_BY_TECHNICIAN("Moved by technician"),
    DATE_RANGE("Date range")
}

data class ReportsUiState(
    val selectedType: ReportScreenType = ReportScreenType.ASSETS_PER_DEPARTMENT,
    val ownerFilter: String = "",
    val technicianFilter: String = "",
    val startMillis: Long = System.currentTimeMillis() - 86400000L * 30,
    val endMillis: Long = System.currentTimeMillis(),
    val report: TabularReport? = null,
    val exportedReport: ExportedReport? = null,
    val loading: Boolean = false,
    val message: String? = null
)

class ReportsViewModel(
    private val buildReportsUseCase: BuildReportsUseCase,
    private val reportRepository: ReportRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        generate()
    }

    fun selectType(type: ReportScreenType) {
        _uiState.update { it.copy(selectedType = type, report = null, exportedReport = null) }
        generate()
    }

    fun updateOwnerFilter(value: String) {
        _uiState.update { it.copy(ownerFilter = value, message = null) }
    }

    fun updateTechnicianFilter(value: String) {
        _uiState.update { it.copy(technicianFilter = value, message = null) }
    }

    fun updateStartDate(value: Long) {
        _uiState.update { it.copy(startMillis = value, message = null) }
    }

    fun updateEndDate(value: Long) {
        _uiState.update { it.copy(endMillis = value, message = null) }
    }

    fun generate() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, message = null, exportedReport = null) }
            val state = _uiState.value
            val report = when (state.selectedType) {
                ReportScreenType.ASSETS_PER_DEPARTMENT -> buildReportsUseCase.assetsPerDepartment()
                ReportScreenType.ASSETS_PER_BUILDING -> buildReportsUseCase.assetsPerBuilding()
                ReportScreenType.MOVEMENT_HISTORY -> buildReportsUseCase.movementsInDateRange(0, System.currentTimeMillis())
                ReportScreenType.ALLOCATED_TO_USER -> buildReportsUseCase.allocatedToUser(state.ownerFilter)
                ReportScreenType.MOVED_BY_TECHNICIAN -> buildReportsUseCase.movedByTechnician(state.technicianFilter)
                ReportScreenType.DATE_RANGE -> buildReportsUseCase.movementsInDateRange(
                    state.startMillis,
                    state.endMillis + 86_399_999L
                )
            }
            _uiState.update { it.copy(report = report, loading = false) }
        }
    }

    fun export(format: ExportFormat, actor: User) {
        val report = _uiState.value.report ?: return
        viewModelScope.launch {
            val exported = reportRepository.export(report, format, actor)
            _uiState.update {
                it.copy(
                    exportedReport = exported,
                    message = "Exported ${exported.fileName} to ${exported.absolutePath}."
                )
            }
        }
    }
}
