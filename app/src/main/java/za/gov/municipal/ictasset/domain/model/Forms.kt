package za.gov.municipal.ictasset.domain.model

data class RegisterAssetRequest(
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

data class MoveAssetRequest(
    val assetBarcode: String,
    val technicianUserId: Long,
    val newOwner: String,
    val newBuildingId: Long?,
    val newOfficeNumber: String,
    val departmentId: Long?,
    val section: String,
    val roomId: Long?,
    val roomBarcode: String,
    val movementDate: Long,
    val movementType: MovementType,
    val reason: String,
    val signatureConfirmation: String,
    val assetPhotoPath: String
)

sealed interface SaveResult {
    data class Success(val id: Long) : SaveResult
    data class Error(val message: String) : SaveResult
}

data class CreateUserRequest(
    val fullName: String,
    val username: String,
    val password: String,
    val role: UserRole
)
