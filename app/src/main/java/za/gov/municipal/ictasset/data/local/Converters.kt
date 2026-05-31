package za.gov.municipal.ictasset.data.local

import androidx.room.TypeConverter
import za.gov.municipal.ictasset.domain.model.AuditAction
import za.gov.municipal.ictasset.domain.model.MovementType
import za.gov.municipal.ictasset.domain.model.UserRole

class Converters {
    @TypeConverter
    fun roleToString(value: UserRole): String = value.name

    @TypeConverter
    fun stringToRole(value: String): UserRole = UserRole.valueOf(value)

    @TypeConverter
    fun movementTypeToString(value: MovementType): String = value.name

    @TypeConverter
    fun stringToMovementType(value: String): MovementType = MovementType.valueOf(value)

    @TypeConverter
    fun auditActionToString(value: AuditAction): String = value.name

    @TypeConverter
    fun stringToAuditAction(value: String): AuditAction = AuditAction.valueOf(value)
}
