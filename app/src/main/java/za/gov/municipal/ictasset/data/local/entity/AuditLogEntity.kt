package za.gov.municipal.ictasset.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import za.gov.municipal.ictasset.domain.model.AuditAction

@Entity(
    tableName = "audit_logs",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["actorUserId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["actorUserId"])]
)
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val actorUserId: Long?,
    val action: AuditAction,
    val entityType: String,
    val entityId: Long?,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)
