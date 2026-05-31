package za.gov.municipal.ictasset.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import za.gov.municipal.ictasset.domain.model.UserRole

@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val username: String,
    val passwordHash: String,
    val role: UserRole,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
