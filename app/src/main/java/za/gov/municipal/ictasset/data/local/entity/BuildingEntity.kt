package za.gov.municipal.ictasset.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "buildings",
    indices = [Index(value = ["name"], unique = true)]
)
data class BuildingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val address: String
)
