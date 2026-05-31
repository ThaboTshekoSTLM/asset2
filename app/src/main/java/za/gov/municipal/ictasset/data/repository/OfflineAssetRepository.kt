package za.gov.municipal.ictasset.data.repository

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import za.gov.municipal.ictasset.data.local.AppDatabase
import za.gov.municipal.ictasset.data.local.LocalDataSeeder
import za.gov.municipal.ictasset.data.local.dao.AssetDao
import za.gov.municipal.ictasset.data.local.dao.AuditLogDao
import za.gov.municipal.ictasset.data.local.dao.MovementDao
import za.gov.municipal.ictasset.data.local.entity.AssetEntity
import za.gov.municipal.ictasset.data.local.entity.AssetMovementEntity
import za.gov.municipal.ictasset.data.local.entity.AuditLogEntity
import za.gov.municipal.ictasset.domain.model.Asset
import za.gov.municipal.ictasset.domain.model.AssetMovement
import za.gov.municipal.ictasset.domain.model.AuditAction
import za.gov.municipal.ictasset.domain.model.DashboardSummary
import za.gov.municipal.ictasset.domain.model.MoveAssetRequest
import za.gov.municipal.ictasset.domain.model.MovementType
import za.gov.municipal.ictasset.domain.model.RegisterAssetRequest
import za.gov.municipal.ictasset.domain.model.SaveResult
import za.gov.municipal.ictasset.domain.model.User
import za.gov.municipal.ictasset.domain.repository.AssetRepository

class OfflineAssetRepository(
    private val database: AppDatabase,
    private val assetDao: AssetDao,
    private val movementDao: MovementDao,
    private val auditLogDao: AuditLogDao,
    private val seeder: LocalDataSeeder
) : AssetRepository {
    override suspend fun seedIfNeeded() {
        seeder.seedIfNeeded()
    }

    override fun observeDashboardSummary(): Flow<DashboardSummary> =
        combine(
            assetDao.observeTotalAssets(),
            movementDao.observeMovementCount(),
            assetDao.observeAllocatedAssets()
        ) { total, moved, allocated ->
            DashboardSummary(
                totalAssets = total,
                movedAssets = moved,
                allocatedAssets = allocated
            )
        }

    override fun observeRecentMovements(limit: Int): Flow<List<AssetMovement>> =
        movementDao.observeRecentMovements(limit).map { rows -> rows.map { it.toDomain() } }

    override fun searchAssets(query: String): Flow<List<Asset>> =
        assetDao.searchAssets(query.trim()).map { rows -> rows.map { it.toDomain() } }

    override fun observeMovementHistory(assetId: Long): Flow<List<AssetMovement>> =
        movementDao.observeMovementHistory(assetId).map { rows -> rows.map { it.toDomain() } }

    override fun observeMovementHistoryByBarcode(assetBarcode: String): Flow<List<AssetMovement>> =
        movementDao.observeMovementHistoryByBarcode(normalizeCode(assetBarcode))
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun findAssetByBarcode(barcode: String): Asset? =
        assetDao.findWithLocationByBarcode(normalizeCode(barcode))?.toDomain()

    override suspend fun findAssetById(id: Long): Asset? =
        assetDao.findWithLocationById(id)?.toDomain()

    override suspend fun registerAsset(request: RegisterAssetRequest, actor: User): SaveResult {
        val validationError = validateRegistration(request)
        if (validationError != null) return SaveResult.Error(validationError)

        val assetBarcode = normalizeCode(request.assetBarcode)
        val serialNumber = normalizeCode(request.serialNumber)
        if (assetDao.countByBarcode(assetBarcode) > 0) {
            return SaveResult.Error("Asset barcode already exists.")
        }
        if (assetDao.countBySerialNumber(serialNumber) > 0) {
            return SaveResult.Error("Serial number already exists.")
        }

        return try {
            val assetId = database.withTransaction {
                val id = assetDao.insert(
                    AssetEntity(
                        deviceDescription = request.deviceDescription.trim(),
                        assetBarcode = assetBarcode,
                        serialNumber = serialNumber,
                        departmentId = request.departmentId,
                        section = request.section.trim(),
                        buildingId = request.buildingId,
                        officeNumber = request.officeNumber.trim(),
                        roomId = request.roomId,
                        roomBarcode = normalizeOptionalCode(request.roomBarcode),
                        currentOwner = request.currentOwner.trim(),
                        previousOwner = request.previousOwner.trim(),
                        technicianResponsibleId = request.technicianResponsibleId ?: actor.id,
                        dateRegistered = request.dateRegistered,
                        dateMoved = request.dateMoved,
                        movementType = request.movementType,
                        notes = request.notes.trim(),
                        assetPhotoPath = request.assetPhotoPath.trim()
                    )
                )

                if (request.currentOwner.isNotBlank()) {
                    // Create the first immutable movement record when a new asset is allocated.
                    movementDao.insert(
                        AssetMovementEntity(
                            assetId = id,
                            technicianUserId = request.technicianResponsibleId ?: actor.id,
                            previousOwner = request.previousOwner.ifBlank { "Stores" }.trim(),
                            newOwner = request.currentOwner.trim(),
                            previousLocation = "Stores",
                            newBuildingId = request.buildingId,
                            newOfficeNumber = request.officeNumber.trim(),
                            departmentId = request.departmentId,
                            section = request.section.trim(),
                            roomId = request.roomId,
                            roomBarcode = normalizeOptionalCode(request.roomBarcode),
                            deviceDescription = request.deviceDescription.trim(),
                            assetBarcode = assetBarcode,
                            serialNumber = serialNumber,
                            movementDate = request.dateMoved ?: request.dateRegistered,
                            movementType = MovementType.NEW_ALLOCATION,
                            reason = "Asset registered and allocated.",
                            signatureConfirmation = actor.fullName
                        )
                    )
                }

                auditLogDao.insert(
                    AuditLogEntity(
                        actorUserId = actor.id,
                        action = AuditAction.CREATE_ASSET,
                        entityType = "asset",
                        entityId = id,
                        details = "Created asset $assetBarcode."
                    )
                )
                id
            }
            SaveResult.Success(assetId)
        } catch (exception: SQLiteConstraintException) {
            SaveResult.Error("Duplicate barcode or serial number blocked by the database.")
        }
    }

    override suspend fun moveAsset(request: MoveAssetRequest, actor: User): SaveResult {
        val assetRow = assetDao.findWithLocationByBarcode(normalizeCode(request.assetBarcode))
            ?: return SaveResult.Error("No asset found for that barcode.")
        val validationError = validateMovement(request)
        if (validationError != null) return SaveResult.Error(validationError)

        val existing = assetRow.asset
        val previousLocation = listOfNotNull(
            assetRow.buildingName,
            existing.officeNumber.takeIf { it.isNotBlank() },
            existing.roomBarcode.ifBlank { assetRow.roomBarcode.orEmpty() }.takeIf { it.isNotBlank() }
        ).joinToString(" / ").ifBlank { "Unknown" }

        return try {
            val movementId = database.withTransaction {
                assetDao.update(
                    existing.copy(
                        departmentId = request.departmentId,
                        section = request.section.trim(),
                        buildingId = request.newBuildingId,
                        officeNumber = request.newOfficeNumber.trim(),
                        roomId = request.roomId,
                        roomBarcode = normalizeOptionalCode(request.roomBarcode),
                        previousOwner = existing.currentOwner,
                        currentOwner = request.newOwner.trim(),
                        technicianResponsibleId = request.technicianUserId,
                        dateMoved = request.movementDate,
                        movementType = request.movementType,
                        assetPhotoPath = request.assetPhotoPath.trim()
                            .ifBlank { existing.assetPhotoPath }
                    )
                )

                val id = movementDao.insert(
                    // Movement rows are append-only; only the current asset snapshot is updated.
                    AssetMovementEntity(
                        assetId = existing.id,
                        technicianUserId = request.technicianUserId,
                        previousOwner = existing.currentOwner,
                        newOwner = request.newOwner.trim(),
                        previousLocation = previousLocation,
                        newBuildingId = request.newBuildingId,
                        newOfficeNumber = request.newOfficeNumber.trim(),
                        departmentId = request.departmentId,
                        section = request.section.trim(),
                        roomId = request.roomId,
                        roomBarcode = normalizeOptionalCode(request.roomBarcode),
                        deviceDescription = existing.deviceDescription,
                        assetBarcode = existing.assetBarcode,
                        serialNumber = existing.serialNumber,
                        movementDate = request.movementDate,
                        movementType = request.movementType,
                        reason = request.reason.trim(),
                        signatureConfirmation = request.signatureConfirmation.trim()
                    )
                )

                auditLogDao.insert(
                    AuditLogEntity(
                        actorUserId = actor.id,
                        action = AuditAction.MOVE_ASSET,
                        entityType = "asset_movement",
                        entityId = id,
                        details = "Moved asset ${existing.assetBarcode} from ${existing.currentOwner} to ${request.newOwner.trim()}."
                    )
                )
                id
            }
            SaveResult.Success(movementId)
        } catch (exception: SQLiteConstraintException) {
            SaveResult.Error("Movement could not be saved because related reference data is missing.")
        }
    }

    private fun validateRegistration(request: RegisterAssetRequest): String? =
        when {
            request.deviceDescription.isBlank() -> "Device description is required."
            request.assetBarcode.isBlank() -> "Asset barcode is required."
            request.serialNumber.isBlank() -> "Serial number is required."
            request.departmentId == null -> "Department is required."
            request.buildingId == null -> "Building is required."
            request.officeNumber.isBlank() -> "Office number is required."
            request.currentOwner.isBlank() -> "Current asset owner is required."
            else -> null
        }

    private fun validateMovement(request: MoveAssetRequest): String? =
        when {
            request.assetBarcode.isBlank() -> "Scan or enter an asset barcode."
            request.technicianUserId <= 0 -> "Technician is required."
            request.newOwner.isBlank() -> "New/current owner is required."
            request.newBuildingId == null -> "New building is required."
            request.newOfficeNumber.isBlank() -> "New office number is required."
            request.departmentId == null -> "Department is required."
            request.reason.isBlank() -> "Reason for movement is required."
            request.signatureConfirmation.isBlank() -> "Signature or confirmation is required."
            else -> null
        }

    private fun normalizeCode(value: String): String = value.trim().uppercase()

    private fun normalizeOptionalCode(value: String): String = value.trim().uppercase()
}
