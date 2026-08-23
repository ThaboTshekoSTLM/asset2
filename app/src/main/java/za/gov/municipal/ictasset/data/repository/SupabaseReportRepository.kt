package za.gov.municipal.ictasset.data.repository

import za.gov.municipal.ictasset.domain.model.AssetsPerBuildingReport
import za.gov.municipal.ictasset.domain.model.AssetsPerDepartmentReport
import za.gov.municipal.ictasset.domain.model.DateRangeMovementReport
import za.gov.municipal.ictasset.domain.model.ExportedReport
import za.gov.municipal.ictasset.domain.model.ExportFormat
import za.gov.municipal.ictasset.domain.model.TabularReport
import za.gov.municipal.ictasset.domain.model.TechnicianMovementReport
import za.gov.municipal.ictasset.domain.model.User
import za.gov.municipal.ictasset.domain.model.UserAllocationReport
import za.gov.municipal.ictasset.domain.repository.ReportRepository

class SupabaseReportRepository(
    private val assets: SupabaseAssetRepository,
    private val reportExporter: ReportExporter
) : ReportRepository {
    override suspend fun assetsPerDepartment(): List<AssetsPerDepartmentReport> =
        assets.currentAssets().groupingBy { it.departmentName ?: "Unassigned" }.eachCount()
            .map { AssetsPerDepartmentReport(it.key, it.value) }.sortedBy { it.departmentName }

    override suspend fun assetsPerBuilding(): List<AssetsPerBuildingReport> =
        assets.currentAssets().groupingBy { it.buildingName ?: "Unassigned" }.eachCount()
            .map { AssetsPerBuildingReport(it.key, it.value) }.sortedBy { it.buildingName }

    override suspend fun assetsAllocatedToUser(ownerQuery: String): List<UserAllocationReport> {
        val query = ownerQuery.trim()
        return assets.currentAssets().filter { query.isBlank() || it.currentOwner.contains(query, true) }
            .groupingBy { it.currentOwner.ifBlank { "Unassigned" } }.eachCount()
            .map { UserAllocationReport(it.key, it.value) }.sortedBy { it.ownerName }
    }

    override suspend fun movementsByTechnician(technicianQuery: String): List<TechnicianMovementReport> {
        val query = technicianQuery.trim()
        return assets.currentMovements().filter { query.isBlank() || it.technicianName.orEmpty().contains(query, true) }
            .groupingBy { it.technicianName ?: "Unassigned" }.eachCount()
            .map { TechnicianMovementReport(it.key, it.value) }.sortedBy { it.technicianName }
    }

    override suspend fun movementsInDateRange(startMillis: Long, endMillis: Long): List<DateRangeMovementReport> =
        assets.currentMovements().filter { it.movementDate in startMillis..endMillis }.map {
            DateRangeMovementReport(
                movementId = it.id,
                assetBarcode = it.assetBarcode,
                serialNumber = it.serialNumber,
                deviceDescription = it.deviceDescription,
                technicianName = it.technicianName,
                previousOwner = it.previousOwner,
                newOwner = it.newOwner,
                movementDate = it.movementDate,
                reason = it.reason
            )
        }

    override suspend fun export(report: TabularReport, format: ExportFormat, actor: User): ExportedReport =
        reportExporter.export(report, format)
}
