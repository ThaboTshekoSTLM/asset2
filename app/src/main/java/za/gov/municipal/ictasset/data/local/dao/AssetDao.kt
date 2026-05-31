package za.gov.municipal.ictasset.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import za.gov.municipal.ictasset.data.local.entity.AssetEntity
import za.gov.municipal.ictasset.data.local.model.AssetWithLocation

@Dao
interface AssetDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(asset: AssetEntity): Long

    @Update
    suspend fun update(asset: AssetEntity)

    @Query("SELECT * FROM assets WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): AssetEntity?

    @Query("SELECT * FROM assets WHERE assetBarcode = :barcode LIMIT 1")
    suspend fun findByBarcode(barcode: String): AssetEntity?

    @Query("SELECT * FROM assets WHERE serialNumber = :serialNumber LIMIT 1")
    suspend fun findBySerialNumber(serialNumber: String): AssetEntity?

    @Query("SELECT COUNT(*) FROM assets WHERE assetBarcode = :barcode")
    suspend fun countByBarcode(barcode: String): Int

    @Query("SELECT COUNT(*) FROM assets WHERE serialNumber = :serialNumber")
    suspend fun countBySerialNumber(serialNumber: String): Int

    @Query(
        """
        SELECT a.*, d.name AS departmentName, b.name AS buildingName,
            r.roomBarcode AS roomBarcodeValue, u.fullName AS technicianName
        FROM assets a
        LEFT JOIN departments d ON d.id = a.departmentId
        LEFT JOIN buildings b ON b.id = a.buildingId
        LEFT JOIN rooms r ON r.id = a.roomId
        LEFT JOIN users u ON u.id = a.technicianResponsibleId
        WHERE a.id = :id
        LIMIT 1
        """
    )
    suspend fun findWithLocationById(id: Long): AssetWithLocation?

    @Query(
        """
        SELECT a.*, d.name AS departmentName, b.name AS buildingName,
            r.roomBarcode AS roomBarcodeValue, u.fullName AS technicianName
        FROM assets a
        LEFT JOIN departments d ON d.id = a.departmentId
        LEFT JOIN buildings b ON b.id = a.buildingId
        LEFT JOIN rooms r ON r.id = a.roomId
        LEFT JOIN users u ON u.id = a.technicianResponsibleId
        WHERE a.assetBarcode = :barcode
        LIMIT 1
        """
    )
    suspend fun findWithLocationByBarcode(barcode: String): AssetWithLocation?

    @Query(
        """
        SELECT a.*, d.name AS departmentName, b.name AS buildingName,
            r.roomBarcode AS roomBarcodeValue, u.fullName AS technicianName
        FROM assets a
        LEFT JOIN departments d ON d.id = a.departmentId
        LEFT JOIN buildings b ON b.id = a.buildingId
        LEFT JOIN rooms r ON r.id = a.roomId
        LEFT JOIN users u ON u.id = a.technicianResponsibleId
        WHERE :query = ''
            OR a.assetBarcode LIKE '%' || :query || '%'
            OR a.serialNumber LIKE '%' || :query || '%'
            OR a.currentOwner LIKE '%' || :query || '%'
            OR a.roomBarcode LIKE '%' || :query || '%'
            OR d.name LIKE '%' || :query || '%'
            OR b.name LIKE '%' || :query || '%'
            OR r.roomBarcode LIKE '%' || :query || '%'
        ORDER BY a.dateRegistered DESC
        """
    )
    fun searchAssets(query: String): Flow<List<AssetWithLocation>>

    @Query("SELECT COUNT(*) FROM assets")
    fun observeTotalAssets(): Flow<Int>

    @Query("SELECT COUNT(*) FROM assets WHERE TRIM(currentOwner) != ''")
    fun observeAllocatedAssets(): Flow<Int>
}
