package za.gov.municipal.ictasset.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import za.gov.municipal.ictasset.data.local.dao.ReferenceDao
import za.gov.municipal.ictasset.domain.model.ReferenceData
import za.gov.municipal.ictasset.domain.model.Room
import za.gov.municipal.ictasset.domain.repository.ReferenceRepository

class OfflineReferenceRepository(
    private val referenceDao: ReferenceDao
) : ReferenceRepository {
    override fun observeReferenceData(): Flow<ReferenceData> =
        combine(
            referenceDao.observeDepartments(),
            referenceDao.observeBuildings(),
            referenceDao.observeRooms()
        ) { departments, buildings, rooms ->
            val buildingNames = buildings.associate { it.id to it.name }
            ReferenceData(
                departments = departments.map { it.toDomain() },
                buildings = buildings.map { it.toDomain() },
                rooms = rooms.map { room -> room.toDomain(buildingNames[room.buildingId]) }
            )
        }

    override suspend fun findRoomByBarcode(roomBarcode: String): Room? {
        val room = referenceDao.findRoomByBarcode(roomBarcode.trim().uppercase()) ?: return null
        val building = referenceDao.findBuildingById(room.buildingId)
        return room.toDomain(building?.name)
    }
}
