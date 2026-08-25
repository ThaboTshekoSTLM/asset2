package za.gov.municipal.ictasset.data.repository

import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import za.gov.municipal.ictasset.data.local.dao.ReferenceDao
import za.gov.municipal.ictasset.data.local.LocalDataSeeder
import za.gov.municipal.ictasset.data.remote.SupabaseApi
import za.gov.municipal.ictasset.domain.model.Asset
import za.gov.municipal.ictasset.domain.model.AssetMovement
import za.gov.municipal.ictasset.domain.model.DashboardSummary
import za.gov.municipal.ictasset.domain.model.MoveAssetRequest
import za.gov.municipal.ictasset.domain.model.MovementType
import za.gov.municipal.ictasset.domain.model.RegisterAssetRequest
import za.gov.municipal.ictasset.domain.model.SaveResult
import za.gov.municipal.ictasset.domain.model.User
import za.gov.municipal.ictasset.domain.repository.AssetRepository

class SupabaseAssetRepository(
    private val api: SupabaseApi,
    private val referenceDao: ReferenceDao,
    private val seeder: LocalDataSeeder
) : AssetRepository {
    private val assets = MutableStateFlow<List<Asset>>(emptyList())
    private val movements = MutableStateFlow<List<AssetMovement>>(emptyList())
    private val remoteAssetIds = mutableMapOf<Long, String>()

    fun currentAssets(): List<Asset> = assets.value
    fun currentMovements(): List<AssetMovement> = movements.value

    override suspend fun seedIfNeeded() {
        seeder.seedIfNeeded()
        if (api.accessToken != null) runCatching { refresh() }
    }

    suspend fun refresh() {
        val departmentIds = referenceDao.allDepartments().associate { it.name to it.id }
        val buildingIds = referenceDao.allBuildings().associate { it.name to it.id }
        val roomIds = referenceDao.allRooms().associate { it.roomBarcode to it.id }
        val assetRows = api.fetchAssets().jsonObjects()
        remoteAssetIds.clear()
        assets.value = assetRows.mapNotNull { row ->
            val remoteId = row.optString("id").takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            val localId = stableLong(remoteId)
            remoteAssetIds[localId] = remoteId
            row.toAsset(localId, departmentIds, buildingIds, roomIds)
        }
        val assetsByRemoteId = assets.value.associateBy { remoteAssetIds[it.id] }
        movements.value = api.fetchMovements().jsonObjects().mapNotNull { row ->
            val assetId = row.optString("asset_id").takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            row.toMovement(assetsByRemoteId[assetId], departmentIds, buildingIds, roomIds)
        }
    }

    override fun observeDashboardSummary(): Flow<DashboardSummary> =
        combine(assets, movements) { assetList, movementList ->
            DashboardSummary(assetList.size, movementList.size, assetList.count { it.currentOwner.isNotBlank() })
        }

    override fun observeRecentMovements(limit: Int): Flow<List<AssetMovement>> = movements.map { it.take(limit) }

    override fun searchAssets(query: String): Flow<List<Asset>> = assets.map { list ->
        val needle = query.trim().lowercase()
        if (needle.isBlank()) emptyList() else list.filter {
            it.assetBarcode.lowercase().contains(needle) ||
                it.serialNumber.lowercase().contains(needle)
        }
    }

    override fun observeMovementHistory(assetId: Long): Flow<List<AssetMovement>> =
        movements.map { list -> list.filter { it.assetId == assetId } }

    override fun observeMovementHistoryByBarcode(assetBarcode: String): Flow<List<AssetMovement>> =
        movements.map { list -> list.filter { it.assetBarcode.equals(assetBarcode.trim(), true) } }

    override suspend fun findAssetByBarcode(barcode: String): Asset? =
        assets.value.firstOrNull { it.assetBarcode.equals(barcode.trim(), true) }

    override suspend fun findAssetById(id: Long): Asset? {
        val asset = assets.value.firstOrNull { it.id == id } ?: return null
        val photoPath = asset.assetPhotoPath?.takeIf { it.isNotBlank() } ?: return asset
        val cachedPath = runCatching { api.downloadAssetPhotoToCache(photoPath) }.getOrNull()
        return asset.copy(assetPhotoPath = cachedPath)
    }

    override suspend fun registerAsset(request: RegisterAssetRequest, actor: User): SaveResult {
        return try {
        val department = request.departmentId?.let { referenceDao.findDepartmentById(it) }
            ?: return SaveResult.Error("Department is required.")
        val building = request.buildingId?.let { referenceDao.findBuildingById(it) }
            ?: return SaveResult.Error("Building is required.")
        if (request.assetBarcode.isBlank() || request.serialNumber.isBlank() || request.currentOwner.isBlank()) {
            return SaveResult.Error("Barcode, serial number, and current owner are required.")
        }
        val remoteId = UUID.randomUUID().toString()
        val userId = api.userId ?: return SaveResult.Error("Please sign in again.")
        val remotePhotoPath = request.assetPhotoPath.takeIf { it.isNotBlank() }
            ?.let { api.uploadCompressedAssetPhoto(it, remoteId) }
        val row = api.insertAsset(JSONObject()
            .put("id", remoteId)
            .put("device_description", request.deviceDescription.trim())
            .put("asset_barcode", request.assetBarcode.trim().uppercase())
            .put("serial_number", request.serialNumber.trim().uppercase())
            .put("department", department.name)
            .put("section", request.section.trim())
            .put("building", building.name)
            .put("office_number", request.officeNumber.trim())
            .put("room_barcode", request.roomBarcode.trim().uppercase())
            .put("current_owner", request.currentOwner.trim())
            .put("previous_owner", request.previousOwner.trim())
            .put("technician", actor.fullName)
            .put("movement_type", request.movementType.apiValue())
            .put("notes", request.notes.trim())
            .put("photo_path", remotePhotoPath ?: JSONObject.NULL)
            .put("created_by", userId)
            .put("updated_by", userId))
        api.insertMovement(JSONObject()
            .put("asset_id", row.getString("id"))
            .put("previous_owner", request.previousOwner.ifBlank { "Stores" })
            .put("new_owner", request.currentOwner.trim())
            .put("previous_location", "Stores")
            .put("new_building", building.name)
            .put("new_office_number", request.officeNumber.trim())
            .put("department", department.name)
            .put("section", request.section.trim())
            .put("room_barcode", request.roomBarcode.trim().uppercase())
            .put("movement_type", "new_allocation")
            .put("reason", "Asset registered and allocated from Android.")
            .put("technician", actor.fullName)
            .put("confirmation", actor.fullName)
            .put("created_by", userId))
        // The asset and its initial movement are already committed. A refresh
        // problem must not report the completed registration as a failure and
        // encourage the user to submit the same barcode again.
        runCatching { refresh() }
        SaveResult.Success(stableLong(remoteId))
    } catch (error: Exception) {
        SaveResult.Error(error.message ?: "Unable to save asset to Supabase.")
        }
    }

    override suspend fun moveAsset(request: MoveAssetRequest, actor: User): SaveResult {
        return try {
        val asset = findAssetByBarcode(request.assetBarcode)
            ?: return SaveResult.Error("No asset found for that barcode.")
        val remoteId = remoteAssetIds[asset.id] ?: return SaveResult.Error("Asset is not synchronized.")
        val department = request.departmentId?.let { referenceDao.findDepartmentById(it) }
            ?: return SaveResult.Error("Department is required.")
        val building = request.newBuildingId?.let { referenceDao.findBuildingById(it) }
            ?: return SaveResult.Error("Building is required.")
        api.recordMovement(JSONObject()
            .put("p_asset_id", remoteId)
            .put("p_new_owner", request.newOwner.trim())
            .put("p_new_building", building.name)
            .put("p_new_office_number", request.newOfficeNumber.trim())
            .put("p_department", department.name)
            .put("p_section", request.section.trim())
            .put("p_room_barcode", request.roomBarcode.trim().uppercase())
            .put("p_movement_type", request.movementType.apiValue())
            .put("p_reason", request.reason.trim())
            .put("p_technician", actor.fullName)
            .put("p_confirmation", request.signatureConfirmation.trim()))
        refresh()
        SaveResult.Success(System.currentTimeMillis())
    } catch (error: Exception) {
        SaveResult.Error(error.message ?: "Unable to record movement in Supabase.")
        }
    }
}

private fun JSONObject.toAsset(
    localId: Long,
    departments: Map<String, Long>,
    buildings: Map<String, Long>,
    rooms: Map<String, Long>
): Asset = Asset(
    id = localId,
    deviceDescription = optString("device_description"),
    assetBarcode = optString("asset_barcode"),
    serialNumber = optString("serial_number"),
    departmentId = departments[optString("department")],
    departmentName = optString("department"),
    section = optString("section"),
    buildingId = buildings[optString("building")],
    buildingName = optString("building"),
    officeNumber = optString("office_number"),
    roomId = rooms[optString("room_barcode")],
    roomBarcode = optString("room_barcode"),
    currentOwner = optString("current_owner"),
    previousOwner = optString("previous_owner"),
    technicianResponsibleId = null,
    technicianResponsibleName = optString("technician"),
    dateRegistered = optInstant("registered_at"),
    dateMoved = optString("moved_at").takeIf { it.isNotBlank() }?.let { Instant.parse(it).toEpochMilli() },
    movementType = movementType(optString("movement_type")),
    notes = optString("notes"),
    assetPhotoPath = optString("photo_path").takeIf { it.isNotBlank() }
)

private fun JSONObject.toMovement(
    asset: Asset?,
    departments: Map<String, Long>,
    buildings: Map<String, Long>,
    rooms: Map<String, Long>
): AssetMovement = AssetMovement(
    id = stableLong(getString("id")),
    assetId = asset?.id ?: stableLong(getString("asset_id")),
    technicianUserId = 0,
    technicianName = optString("technician"),
    previousOwner = optString("previous_owner"),
    newOwner = optString("new_owner"),
    previousLocation = optString("previous_location"),
    newBuildingId = buildings[optString("new_building")],
    newBuildingName = optString("new_building"),
    newOfficeNumber = optString("new_office_number"),
    departmentId = departments[optString("department")],
    departmentName = optString("department"),
    section = optString("section"),
    roomId = rooms[optString("room_barcode")],
    roomBarcode = optString("room_barcode"),
    deviceDescription = asset?.deviceDescription.orEmpty(),
    assetBarcode = asset?.assetBarcode.orEmpty(),
    serialNumber = asset?.serialNumber.orEmpty(),
    movementDate = optInstant("movement_date"),
    movementType = movementType(optString("movement_type")),
    reason = optString("reason"),
    signatureConfirmation = optString("confirmation")
)

private fun JSONObject.optInstant(name: String): Long =
    optString(name).takeIf { it.isNotBlank() && it != "null" }
        ?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrDefault(0L) }
        ?: 0L

private fun MovementType.apiValue(): String = name.lowercase()

private fun movementType(value: String): MovementType =
    runCatching { MovementType.valueOf(value.uppercase()) }.getOrDefault(MovementType.TRANSFER)
