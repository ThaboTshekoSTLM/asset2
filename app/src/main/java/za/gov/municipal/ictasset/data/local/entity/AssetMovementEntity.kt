package za.gov.municipal.ictasset.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import za.gov.municipal.ictasset.domain.model.MovementType

@Entity(
    tableName = "asset_movements",
    foreignKeys = [
        ForeignKey(
            entity = AssetEntity::class,
            parentColumns = ["id"],
            childColumns = ["assetId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["technicianUserId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = DepartmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["departmentId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = BuildingEntity::class,
            parentColumns = ["id"],
            childColumns = ["newBuildingId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = RoomEntity::class,
            parentColumns = ["id"],
            childColumns = ["roomId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["assetId"]),
        Index(value = ["technicianUserId"]),
        Index(value = ["departmentId"]),
        Index(value = ["newBuildingId"]),
        Index(value = ["roomId"])
    ]
)
data class AssetMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val assetId: Long,
    val technicianUserId: Long,
    val previousOwner: String,
    val newOwner: String,
    val previousLocation: String,
    val newBuildingId: Long?,
    val newOfficeNumber: String,
    val departmentId: Long?,
    val section: String,
    val roomId: Long?,
    val roomBarcode: String,
    val deviceDescription: String,
    val assetBarcode: String,
    val serialNumber: String,
    val movementDate: Long,
    val movementType: MovementType,
    val reason: String,
    val signatureConfirmation: String,
    val createdAt: Long = System.currentTimeMillis()
)
