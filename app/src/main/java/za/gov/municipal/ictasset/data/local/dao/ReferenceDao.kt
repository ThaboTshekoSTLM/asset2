package za.gov.municipal.ictasset.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import za.gov.municipal.ictasset.data.local.entity.BuildingEntity
import za.gov.municipal.ictasset.data.local.entity.DepartmentEntity
import za.gov.municipal.ictasset.data.local.entity.RoomEntity
import za.gov.municipal.ictasset.data.local.model.AssetCountByBuildingRow
import za.gov.municipal.ictasset.data.local.model.AssetCountByDepartmentRow

@Dao
interface ReferenceDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDepartments(departments: List<DepartmentEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBuildings(buildings: List<BuildingEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRooms(rooms: List<RoomEntity>): List<Long>

    @Query("SELECT * FROM departments ORDER BY name, section")
    fun observeDepartments(): Flow<List<DepartmentEntity>>

    @Query("SELECT * FROM buildings ORDER BY name")
    fun observeBuildings(): Flow<List<BuildingEntity>>

    @Query("SELECT * FROM rooms ORDER BY officeNumber")
    fun observeRooms(): Flow<List<RoomEntity>>

    @Query("SELECT * FROM rooms WHERE roomBarcode = :roomBarcode LIMIT 1")
    suspend fun findRoomByBarcode(roomBarcode: String): RoomEntity?

    @Query("SELECT * FROM buildings WHERE id = :id LIMIT 1")
    suspend fun findBuildingById(id: Long): BuildingEntity?

    @Query("SELECT * FROM departments WHERE id = :id LIMIT 1")
    suspend fun findDepartmentById(id: Long): DepartmentEntity?

    @Query("SELECT * FROM departments")
    suspend fun allDepartments(): List<DepartmentEntity>

    @Query("SELECT * FROM buildings")
    suspend fun allBuildings(): List<BuildingEntity>

    @Query("SELECT * FROM rooms")
    suspend fun allRooms(): List<RoomEntity>

    @Query(
        """
        SELECT COALESCE(d.name, 'Unassigned') AS departmentName, COUNT(a.id) AS totalAssets
        FROM departments d
        LEFT JOIN assets a ON a.departmentId = d.id
        GROUP BY d.id
        UNION ALL
        SELECT 'Unassigned' AS departmentName, COUNT(*) AS totalAssets
        FROM assets
        WHERE departmentId IS NULL
        ORDER BY departmentName
        """
    )
    suspend fun assetsPerDepartment(): List<AssetCountByDepartmentRow>

    @Query(
        """
        SELECT COALESCE(b.name, 'Unassigned') AS buildingName, COUNT(a.id) AS totalAssets
        FROM buildings b
        LEFT JOIN assets a ON a.buildingId = b.id
        GROUP BY b.id
        UNION ALL
        SELECT 'Unassigned' AS buildingName, COUNT(*) AS totalAssets
        FROM assets
        WHERE buildingId IS NULL
        ORDER BY buildingName
        """
    )
    suspend fun assetsPerBuilding(): List<AssetCountByBuildingRow>

    @Query("SELECT COUNT(*) FROM departments")
    suspend fun departmentCount(): Int
}
