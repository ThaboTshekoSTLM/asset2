package za.gov.municipal.ictasset.data.repository

import za.gov.municipal.ictasset.data.local.dao.AuditLogDao
import za.gov.municipal.ictasset.data.local.dao.ReferenceDao
import za.gov.municipal.ictasset.data.local.dao.ReportDao
import za.gov.municipal.ictasset.data.local.entity.AuditLogEntity
import za.gov.municipal.ictasset.domain.model.AssetsPerBuildingReport
import za.gov.municipal.ictasset.domain.model.AssetsPerDepartmentReport
import za.gov.municipal.ictasset.domain.model.AuditAction
import za.gov.municipal.ictasset.domain.model.DateRangeMovementReport
import za.gov.municipal.ictasset.domain.model.ExportedReport
import za.gov.municipal.ictasset.domain.model.ExportFormat
import za.gov.municipal.ictasset.domain.model.TabularReport
import za.gov.municipal.ictasset.domain.model.TechnicianMovementReport
import za.gov.municipal.ictasset.domain.model.User
import za.gov.municipal.ictasset.domain.model.UserAllocationReport
import za.gov.municipal.ictasset.domain.repository.ReportRepository

class OfflineReportRepository(
    private val referenceDao: ReferenceDao,
    private val reportDao: ReportDao,
    private val auditLogDao: AuditLogDao,
    private val reportExporter: ReportExporter
) : ReportRepository {
    override suspend fun assetsPerDepartment(): List<AssetsPerDepartmentReport> =
        referenceDao.assetsPerDepartment().map {
            AssetsPerDepartmentReport(
                departmentName = it.departmentName.orEmpty(),
                totalAssets = it.totalAssets
            )
        }

    override suspend fun assetsPerBuilding(): List<AssetsPerBuildingReport> =
        referenceDao.assetsPerBuilding().map {
            AssetsPerBuildingReport(
                buildingName = it.buildingName.orEmpty(),
                totalAssets = it.totalAssets
            )
        }

    override suspend fun assetsAllocatedToUser(ownerQuery: String): List<UserAllocationReport> =
        reportDao.assetsAllocatedToUser(ownerQuery.trim()).map {
            UserAllocationReport(
                ownerName = it.ownerName.orEmpty(),
                totalAssets = it.totalAssets
            )
        }

    override suspend fun movementsByTechnician(technicianQuery: String): List<TechnicianMovementReport> =
        reportDao.movementsByTechnician(technicianQuery.trim()).map {
            TechnicianMovementReport(
                technicianName = it.technicianName.orEmpty(),
                totalMovements = it.totalMovements
            )
        }

    override suspend fun movementsInDateRange(
        startMillis: Long,
        endMillis: Long
    ): List<DateRangeMovementReport> =
        reportDao.movementsInDateRange(startMillis, endMillis).map {
            DateRangeMovementReport(
                movementId = it.movementId,
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

    override suspend fun export(
        report: TabularReport,
        format: ExportFormat,
        actor: User
    ): ExportedReport {
        val exported = reportExporter.export(report, format)
        auditLogDao.insert(
            AuditLogEntity(
                actorUserId = actor.id,
                action = AuditAction.EXPORT_REPORT,
                entityType = "report",
                entityId = null,
                details = "Exported ${report.title} to ${format.name}: ${exported.fileName}."
            )
        )
        return exported
    }
}
