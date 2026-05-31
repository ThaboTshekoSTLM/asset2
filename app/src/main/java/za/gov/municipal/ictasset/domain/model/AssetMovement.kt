package za.gov.municipal.ictasset.domain.model

data class AssetMovement(
    val id: Long,
    val assetId: Long,
    val technicianUserId: Long,
    val technicianName: String?,
    val previousOwner: String,
    val newOwner: String,
    val previousLocation: String,
    val newBuildingId: Long?,
    val newBuildingName: String?,
    val newOfficeNumber: String,
    val departmentId: Long?,
    val departmentName: String?,
    val section: String,
    val roomId: Long?,
    val roomBarcode: String?,
    val deviceDescription: String,
    val assetBarcode: String,
    val serialNumber: String,
    val movementDate: Long,
    val movementType: MovementType,
    val reason: String,
    val signatureConfirmation: String
)
