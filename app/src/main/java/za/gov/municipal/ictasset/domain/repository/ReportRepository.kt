package za.gov.municipal.ictasset.domain.repository

import za.gov.municipal.ictasset.domain.model.AssetsPerBuildingReport
import za.gov.municipal.ictasset.domain.model.AssetsPerDepartmentReport
import za.gov.municipal.ictasset.domain.model.DateRangeMovementReport
import za.gov.municipal.ictasset.domain.model.ExportedReport
import za.gov.municipal.ictasset.domain.model.ExportFormat
import za.gov.municipal.ictasset.domain.model.TabularReport
import za.gov.municipal.ictasset.domain.model.TechnicianMovementReport
import za.gov.municipal.ictasset.domain.model.User
import za.gov.municipal.ictasset.domain.model.UserAllocationReport

interface ReportRepository {
    suspend fun assetsPerDepartment(): List<AssetsPerDepartmentReport>
    suspend fun assetsPerBuilding(): List<AssetsPerBuildingReport>
    suspend fun assetsAllocatedToUser(ownerQuery: String): List<UserAllocationReport>
    suspend fun movementsByTechnician(technicianQuery: String): List<TechnicianMovementReport>
    suspend fun movementsInDateRange(startMillis: Long, endMillis: Long): List<DateRangeMovementReport>
    suspend fun export(report: TabularReport, format: ExportFormat, actor: User): ExportedReport
}
