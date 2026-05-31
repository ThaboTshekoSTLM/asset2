package za.gov.municipal.ictasset.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import za.gov.municipal.ictasset.data.local.entity.AssetMovementEntity

data class MovementWithLocation(
    @Embedded val movement: AssetMovementEntity,
    @ColumnInfo(name = "departmentName") val departmentName: String?,
    @ColumnInfo(name = "buildingName") val buildingName: String?,
    @ColumnInfo(name = "roomBarcodeValue") val roomBarcode: String?,
    @ColumnInfo(name = "technicianName") val technicianName: String?
)
