package za.gov.municipal.ictasset.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import za.gov.municipal.ictasset.data.local.model.DateRangeMovementRow
import za.gov.municipal.ictasset.data.local.model.TechnicianMovementRow
import za.gov.municipal.ictasset.data.local.model.UserAllocationRow

@Dao
interface ReportDao {
    @Query(
        """
        SELECT COALESCE(currentOwner, 'Unassigned') AS ownerName, COUNT(*) AS totalAssets
        FROM assets
        WHERE :ownerQuery = '' OR currentOwner LIKE '%' || :ownerQuery || '%'
        GROUP BY currentOwner
        ORDER BY ownerName
        """
    )
    suspend fun assetsAllocatedToUser(ownerQuery: String): List<UserAllocationRow>

    @Query(
        """
        SELECT COALESCE(u.fullName, 'Unknown technician') AS technicianName,
            COUNT(m.id) AS totalMovements
        FROM asset_movements m
        LEFT JOIN users u ON u.id = m.technicianUserId
        WHERE :technicianQuery = '' OR u.fullName LIKE '%' || :technicianQuery || '%'
        GROUP BY m.technicianUserId
        ORDER BY technicianName
        """
    )
    suspend fun movementsByTechnician(technicianQuery: String): List<TechnicianMovementRow>

    @Query(
        """
        SELECT m.id AS movementId, m.assetBarcode, m.serialNumber, m.deviceDescription,
            u.fullName AS technicianName, m.previousOwner, m.newOwner,
            m.movementDate, m.reason
        FROM asset_movements m
        LEFT JOIN users u ON u.id = m.technicianUserId
        WHERE m.movementDate BETWEEN :startMillis AND :endMillis
        ORDER BY m.movementDate DESC
        """
    )
    suspend fun movementsInDateRange(startMillis: Long, endMillis: Long): List<DateRangeMovementRow>
}
