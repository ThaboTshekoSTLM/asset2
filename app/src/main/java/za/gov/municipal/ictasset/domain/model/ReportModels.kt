package za.gov.municipal.ictasset.domain.model

data class AssetsPerDepartmentReport(
    val departmentName: String,
    val totalAssets: Int
)

data class AssetsPerBuildingReport(
    val buildingName: String,
    val totalAssets: Int
)

data class TechnicianMovementReport(
    val technicianName: String,
    val totalMovements: Int
)

data class UserAllocationReport(
    val ownerName: String,
    val totalAssets: Int
)

data class DateRangeMovementReport(
    val movementId: Long,
    val assetBarcode: String,
    val serialNumber: String,
    val deviceDescription: String,
    val technicianName: String?,
    val previousOwner: String,
    val newOwner: String,
    val movementDate: Long,
    val reason: String
)
