package za.gov.municipal.ictasset.domain.model

data class Asset(
    val id: Long,
    val deviceDescription: String,
    val assetBarcode: String,
    val serialNumber: String,
    val departmentId: Long?,
    val departmentName: String?,
    val section: String,
    val buildingId: Long?,
    val buildingName: String?,
    val officeNumber: String,
    val roomId: Long?,
    val roomBarcode: String?,
    val currentOwner: String,
    val previousOwner: String,
    val technicianResponsibleId: Long?,
    val technicianResponsibleName: String?,
    val dateRegistered: Long,
    val dateMoved: Long?,
    val movementType: MovementType,
    val notes: String,
    val assetPhotoPath: String?
)
