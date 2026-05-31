package za.gov.municipal.ictasset.data.local

import androidx.room.withTransaction
import za.gov.municipal.ictasset.data.local.dao.AssetDao
import za.gov.municipal.ictasset.data.local.dao.AuditLogDao
import za.gov.municipal.ictasset.data.local.dao.MovementDao
import za.gov.municipal.ictasset.data.local.dao.ReferenceDao
import za.gov.municipal.ictasset.data.local.dao.UserDao
import za.gov.municipal.ictasset.data.local.entity.AssetEntity
import za.gov.municipal.ictasset.data.local.entity.AssetMovementEntity
import za.gov.municipal.ictasset.data.local.entity.AuditLogEntity
import za.gov.municipal.ictasset.data.local.entity.BuildingEntity
import za.gov.municipal.ictasset.data.local.entity.DepartmentEntity
import za.gov.municipal.ictasset.data.local.entity.RoomEntity
import za.gov.municipal.ictasset.data.local.entity.UserEntity
import za.gov.municipal.ictasset.domain.model.AuditAction
import za.gov.municipal.ictasset.domain.model.MovementType
import za.gov.municipal.ictasset.domain.model.UserRole

class LocalDataSeeder(
    private val database: AppDatabase,
    private val userDao: UserDao,
    private val referenceDao: ReferenceDao,
    private val assetDao: AssetDao,
    private val movementDao: MovementDao,
    private val auditLogDao: AuditLogDao
) {
    suspend fun seedIfNeeded() {
        if (userDao.count() > 0) return

        database.withTransaction {
            val adminId = userDao.insert(
                UserEntity(
                    fullName = "Municipal ICT Admin",
                    username = "admin",
                    passwordHash = PasswordHasher.hash("admin123"),
                    role = UserRole.ADMIN
                )
            )
            val technicianId = userDao.insert(
                UserEntity(
                    fullName = "Thabo Mokoena",
                    username = "tech",
                    passwordHash = PasswordHasher.hash("tech123"),
                    role = UserRole.ICT_TECHNICIAN
                )
            )
            userDao.insert(
                UserEntity(
                    fullName = "Standard Field User",
                    username = "standard",
                    passwordHash = PasswordHasher.hash("user123"),
                    role = UserRole.STANDARD_USER
                )
            )
            userDao.insert(
                UserEntity(
                    fullName = "Internal Auditor",
                    username = "auditor",
                    passwordHash = PasswordHasher.hash("audit123"),
                    role = UserRole.VIEWER_AUDITOR
                )
            )

            val departmentIds = referenceDao.insertDepartments(
                listOf(
                    DepartmentEntity(name = "Corporate Services", section = "ICT Operations"),
                    DepartmentEntity(name = "Finance", section = "Revenue"),
                    DepartmentEntity(name = "Community Services", section = "Libraries"),
                    DepartmentEntity(name = "Infrastructure", section = "Roads")
                )
            )

            val buildingIds = referenceDao.insertBuildings(
                listOf(
                    BuildingEntity(name = "Civic Centre", address = "Main municipal campus"),
                    BuildingEntity(name = "Finance Building", address = "West wing"),
                    BuildingEntity(name = "Depot Offices", address = "Works depot")
                )
            )

            val roomIds = referenceDao.insertRooms(
                listOf(
                    RoomEntity(
                        buildingId = buildingIds[0],
                        officeNumber = "ICT-101",
                        roomBarcode = "ROOM-CIVIC-ICT101",
                        description = "ICT support office"
                    ),
                    RoomEntity(
                        buildingId = buildingIds[0],
                        officeNumber = "A-214",
                        roomBarcode = "ROOM-CIVIC-A214",
                        description = "Corporate Services open office"
                    ),
                    RoomEntity(
                        buildingId = buildingIds[1],
                        officeNumber = "FIN-03",
                        roomBarcode = "ROOM-FIN-003",
                        description = "Revenue office"
                    )
                )
            )

            val now = System.currentTimeMillis()
            val laptopId = assetDao.insert(
                AssetEntity(
                    deviceDescription = "Dell Latitude 5440 laptop",
                    assetBarcode = "ICT-LAP-0001",
                    serialNumber = "DL5440ZA001",
                    departmentId = departmentIds[0],
                    section = "ICT Operations",
                    buildingId = buildingIds[0],
                    officeNumber = "ICT-101",
                    roomId = roomIds[0],
                    roomBarcode = "ROOM-CIVIC-ICT101",
                    currentOwner = "Thabo Mokoena",
                    previousOwner = "Stores",
                    technicianResponsibleId = technicianId,
                    dateRegistered = now - 86400000L * 7,
                    dateMoved = now - 86400000L * 6,
                    movementType = MovementType.NEW_ALLOCATION,
                    notes = "Sample laptop issued to ICT technician.",
                    assetPhotoPath = ""
                )
            )

            assetDao.insert(
                AssetEntity(
                    deviceDescription = "HP LaserJet Pro printer",
                    assetBarcode = "ICT-PRN-0042",
                    serialNumber = "HPLJPRO042ZA",
                    departmentId = departmentIds[1],
                    section = "Revenue",
                    buildingId = buildingIds[1],
                    officeNumber = "FIN-03",
                    roomId = roomIds[2],
                    roomBarcode = "ROOM-FIN-003",
                    currentOwner = "Finance Shared Office",
                    previousOwner = "Stores",
                    technicianResponsibleId = technicianId,
                    dateRegistered = now - 86400000L * 3,
                    dateMoved = now - 86400000L * 2,
                    movementType = MovementType.NEW_ALLOCATION,
                    notes = "Network printer for revenue team.",
                    assetPhotoPath = ""
                )
            )

            movementDao.insert(
                AssetMovementEntity(
                    assetId = laptopId,
                    technicianUserId = technicianId,
                    previousOwner = "Stores",
                    newOwner = "Thabo Mokoena",
                    previousLocation = "Stores",
                    newBuildingId = buildingIds[0],
                    newOfficeNumber = "ICT-101",
                    departmentId = departmentIds[0],
                    section = "ICT Operations",
                    roomId = roomIds[0],
                    roomBarcode = "ROOM-CIVIC-ICT101",
                    deviceDescription = "Dell Latitude 5440 laptop",
                    assetBarcode = "ICT-LAP-0001",
                    serialNumber = "DL5440ZA001",
                    movementDate = now - 86400000L * 6,
                    movementType = MovementType.NEW_ALLOCATION,
                    reason = "Initial allocation after asset registration.",
                    signatureConfirmation = "Thabo Mokoena"
                )
            )

            auditLogDao.insert(
                AuditLogEntity(
                    actorUserId = adminId,
                    action = AuditAction.SEED_DATABASE,
                    entityType = "database",
                    entityId = null,
                    details = "Inserted sample users, reference data, assets, and a sample movement."
                )
            )
        }
    }
}
