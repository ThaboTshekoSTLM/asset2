package za.gov.municipal.ictasset.data.local.model

data class AssetCountByDepartmentRow(
    val departmentName: String?,
    val totalAssets: Int
)

data class AssetCountByBuildingRow(
    val buildingName: String?,
    val totalAssets: Int
)

data class TechnicianMovementRow(
    val technicianName: String?,
    val totalMovements: Int
)

data class UserAllocationRow(
    val ownerName: String?,
    val totalAssets: Int
)

data class DateRangeMovementRow(
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
