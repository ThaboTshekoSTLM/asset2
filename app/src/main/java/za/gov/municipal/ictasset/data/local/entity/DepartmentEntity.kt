package za.gov.municipal.ictasset.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "departments",
    indices = [Index(value = ["name", "section"], unique = true)]
)
data class DepartmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val section: String
)
