package za.gov.municipal.ictasset.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import za.gov.municipal.ictasset.data.local.entity.AssetMovementEntity
import za.gov.municipal.ictasset.data.local.model.MovementWithLocation

@Dao
interface MovementDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(movement: AssetMovementEntity): Long

    @Query(
        """
        SELECT m.*, d.name AS departmentName, b.name AS buildingName,
            r.roomBarcode AS roomBarcodeValue, u.fullName AS technicianName
        FROM asset_movements m
        LEFT JOIN departments d ON d.id = m.departmentId
        LEFT JOIN buildings b ON b.id = m.newBuildingId
        LEFT JOIN rooms r ON r.id = m.roomId
        LEFT JOIN users u ON u.id = m.technicianUserId
        ORDER BY m.movementDate DESC
        LIMIT :limit
        """
    )
    fun observeRecentMovements(limit: Int): Flow<List<MovementWithLocation>>

    @Query(
        """
        SELECT m.*, d.name AS departmentName, b.name AS buildingName,
            r.roomBarcode AS roomBarcodeValue, u.fullName AS technicianName
        FROM asset_movements m
        LEFT JOIN departments d ON d.id = m.departmentId
        LEFT JOIN buildings b ON b.id = m.newBuildingId
        LEFT JOIN rooms r ON r.id = m.roomId
        LEFT JOIN users u ON u.id = m.technicianUserId
        WHERE m.assetId = :assetId
        ORDER BY m.movementDate DESC
        """
    )
    fun observeMovementHistory(assetId: Long): Flow<List<MovementWithLocation>>

    @Query(
        """
        SELECT m.*, d.name AS departmentName, b.name AS buildingName,
            r.roomBarcode AS roomBarcodeValue, u.fullName AS technicianName
        FROM asset_movements m
        LEFT JOIN departments d ON d.id = m.departmentId
        LEFT JOIN buildings b ON b.id = m.newBuildingId
        LEFT JOIN rooms r ON r.id = m.roomId
        LEFT JOIN users u ON u.id = m.technicianUserId
        WHERE m.assetBarcode = :assetBarcode
        ORDER BY m.movementDate DESC
        """
    )
    fun observeMovementHistoryByBarcode(assetBarcode: String): Flow<List<MovementWithLocation>>

    @Query("SELECT COUNT(*) FROM asset_movements")
    fun observeMovementCount(): Flow<Int>
}
