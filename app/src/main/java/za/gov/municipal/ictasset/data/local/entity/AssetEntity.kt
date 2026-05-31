package za.gov.municipal.ictasset.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import za.gov.municipal.ictasset.domain.model.MovementType

@Entity(
    tableName = "assets",
    foreignKeys = [
        ForeignKey(
            entity = DepartmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["departmentId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = BuildingEntity::class,
            parentColumns = ["id"],
            childColumns = ["buildingId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = RoomEntity::class,
            parentColumns = ["id"],
            childColumns = ["roomId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["technicianResponsibleId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["assetBarcode"], unique = true),
        Index(value = ["serialNumber"], unique = true),
        Index(value = ["departmentId"]),
        Index(value = ["buildingId"]),
        Index(value = ["roomId"]),
        Index(value = ["technicianResponsibleId"])
    ]
)
data class AssetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceDescription: String,
    val assetBarcode: String,
    val serialNumber: String,
    val departmentId: Long?,
    val section: String,
    val buildingId: Long?,
    val officeNumber: String,
    val roomId: Long?,
    val roomBarcode: String,
    val currentOwner: String,
    val previousOwner: String,
    val technicianResponsibleId: Long?,
    val dateRegistered: Long,
    val dateMoved: Long?,
    val movementType: MovementType,
    val notes: String,
    val assetPhotoPath: String
)
