package za.gov.municipal.ictasset.domain.usecase

import za.gov.municipal.ictasset.domain.model.DateRangeMovementReport
import za.gov.municipal.ictasset.domain.model.TabularReport
import za.gov.municipal.ictasset.domain.repository.ReportRepository
import za.gov.municipal.ictasset.domain.util.DateText

class BuildReportsUseCase(
    private val reportRepository: ReportRepository
) {
    suspend fun assetsPerDepartment(): TabularReport =
        TabularReport(
            title = "Assets per department",
            headers = listOf("Department", "Total assets"),
            rows = reportRepository.assetsPerDepartment().map {
                listOf(it.departmentName, it.totalAssets.toString())
            }
        )

    suspend fun assetsPerBuilding(): TabularReport =
        TabularReport(
            title = "Assets per building",
            headers = listOf("Building", "Total assets"),
            rows = reportRepository.assetsPerBuilding().map {
                listOf(it.buildingName, it.totalAssets.toString())
            }
        )

    suspend fun allocatedToUser(ownerQuery: String): TabularReport =
        TabularReport(
            title = "Assets allocated to user",
            headers = listOf("Owner", "Total assets"),
            rows = reportRepository.assetsAllocatedToUser(ownerQuery).map {
                listOf(it.ownerName, it.totalAssets.toString())
            }
        )

    suspend fun movedByTechnician(technicianQuery: String): TabularReport =
        TabularReport(
            title = "Assets moved by technician",
            headers = listOf("Technician", "Total movements"),
            rows = reportRepository.movementsByTechnician(technicianQuery).map {
                listOf(it.technicianName, it.totalMovements.toString())
            }
        )

    suspend fun movementsInDateRange(startMillis: Long, endMillis: Long): TabularReport =
        TabularReport(
            title = "Asset movement history",
            headers = listOf(
                "Movement ID",
                "Asset barcode",
                "Serial number",
                "Device",
                "Technician",
                "Previous owner",
                "New owner",
                "Movement date",
                "Reason"
            ),
            rows = reportRepository.movementsInDateRange(startMillis, endMillis).map { it.toRow() }
        )

    private fun DateRangeMovementReport.toRow(): List<String> =
        listOf(
            movementId.toString(),
            assetBarcode,
            serialNumber,
            deviceDescription,
            technicianName.orEmpty(),
            previousOwner,
            newOwner,
            DateText.dateTime(movementDate),
            reason
        )
}
