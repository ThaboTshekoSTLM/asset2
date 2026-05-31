package za.gov.municipal.ictasset.presentation.reports

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import za.gov.municipal.ictasset.domain.model.TabularReport
import za.gov.municipal.ictasset.presentation.components.AppDropdown
import za.gov.municipal.ictasset.presentation.components.AppTextField
import za.gov.municipal.ictasset.presentation.components.DateField
import za.gov.municipal.ictasset.presentation.components.MessageBanner
import za.gov.municipal.ictasset.presentation.components.SectionTitle

@Composable
fun ReportsScreen(
    state: ReportsUiState,
    onSelectType: (ReportScreenType) -> Unit,
    onOwnerFilterChange: (String) -> Unit,
    onTechnicianFilterChange: (String) -> Unit,
    onStartDateChange: (Long) -> Unit,
    onEndDateChange: (Long) -> Unit,
    onGenerate: () -> Unit,
    onExportPdf: () -> Unit,
    onExportExcel: () -> Unit
) {
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Reports", style = MaterialTheme.typography.titleLarge)
        }
        item {
            AppDropdown(
                label = "Report type",
                selected = state.selectedType,
                options = ReportScreenType.entries,
                optionLabel = { it.label },
                onSelected = onSelectType
            )
        }
        item {
            ReportFilters(
                state = state,
                onOwnerFilterChange = onOwnerFilterChange,
                onTechnicianFilterChange = onTechnicianFilterChange,
                onStartDateChange = onStartDateChange,
                onEndDateChange = onEndDateChange
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onGenerate,
                    enabled = !state.loading,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Text(
                        text = if (state.loading) "Loading..." else "Load preview",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                OutlinedButton(
                    onClick = onExportPdf,
                    enabled = state.report != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Text("PDF", modifier = Modifier.padding(start = 8.dp))
                }
                OutlinedButton(
                    onClick = onExportExcel,
                    enabled = state.report != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                    Text("Excel", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        item {
            MessageBanner(message = state.message ?: state.exportedReport?.absolutePath)
        }
        item {
            SectionTitle(state.report?.let { "Preview: ${it.title}" } ?: "Report preview")
        }
        state.report?.let { report ->
            item { ReportHeader(report) }
            itemsIndexed(report.rows) { index, row ->
                ReportRow(index = index, report = report, row = row)
            }
        } ?: item {
            Text("Choose a report type and load the preview before downloading.")
        }
    }
}

@Composable
private fun ReportFilters(
    state: ReportsUiState,
    onOwnerFilterChange: (String) -> Unit,
    onTechnicianFilterChange: (String) -> Unit,
    onStartDateChange: (Long) -> Unit,
    onEndDateChange: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (state.selectedType == ReportScreenType.ALLOCATED_TO_USER) {
            AppTextField(
                value = state.ownerFilter,
                label = "Owner filter",
                onValueChange = onOwnerFilterChange
            )
        }
        if (state.selectedType == ReportScreenType.MOVED_BY_TECHNICIAN) {
            AppTextField(
                value = state.technicianFilter,
                label = "Technician filter",
                onValueChange = onTechnicianFilterChange
            )
        }
        if (state.selectedType == ReportScreenType.DATE_RANGE) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                DateField(
                    label = "Start date",
                    valueMillis = state.startMillis,
                    onValueChange = onStartDateChange,
                    modifier = Modifier.weight(1f)
                )
                DateField(
                    label = "End date",
                    valueMillis = state.endMillis,
                    onValueChange = onEndDateChange,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ReportHeader(report: TabularReport) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        report.headers.forEach { header ->
            Text(
                text = header,
                modifier = Modifier.padding(vertical = 4.dp),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ReportRow(index: Int, report: TabularReport, row: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (index % 2 == 0) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            row.forEachIndexed { cellIndex, cell ->
                val label = report.headers.getOrNull(cellIndex).orEmpty()
                Text("$label: $cell", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
