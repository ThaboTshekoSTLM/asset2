package za.gov.municipal.ictasset.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import za.gov.municipal.ictasset.data.local.dao.AssetDao
import za.gov.municipal.ictasset.data.local.dao.AuditLogDao
import za.gov.municipal.ictasset.data.local.dao.MovementDao
import za.gov.municipal.ictasset.data.local.dao.ReferenceDao
import za.gov.municipal.ictasset.data.local.dao.ReportDao
import za.gov.municipal.ictasset.data.local.dao.UserDao
import za.gov.municipal.ictasset.data.local.entity.AssetEntity
import za.gov.municipal.ictasset.data.local.entity.AssetMovementEntity
import za.gov.municipal.ictasset.data.local.entity.AuditLogEntity
import za.gov.municipal.ictasset.data.local.entity.BuildingEntity
import za.gov.municipal.ictasset.data.local.entity.DepartmentEntity
import za.gov.municipal.ictasset.data.local.entity.RoomEntity
import za.gov.municipal.ictasset.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        AssetEntity::class,
        AssetMovementEntity::class,
        DepartmentEntity::class,
        BuildingEntity::class,
        RoomEntity::class,
        AuditLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    // Single offline source of truth. A future sync layer can wrap repositories without replacing the UI.
    abstract fun userDao(): UserDao
    abstract fun assetDao(): AssetDao
    abstract fun movementDao(): MovementDao
    abstract fun referenceDao(): ReferenceDao
    abstract fun reportDao(): ReportDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ict_asset_register.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
