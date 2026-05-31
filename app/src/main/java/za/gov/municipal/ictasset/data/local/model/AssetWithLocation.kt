package za.gov.municipal.ictasset.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import za.gov.municipal.ictasset.data.local.entity.AssetEntity

data class AssetWithLocation(
    @Embedded val asset: AssetEntity,
    @ColumnInfo(name = "departmentName") val departmentName: String?,
    @ColumnInfo(name = "buildingName") val buildingName: String?,
    @ColumnInfo(name = "roomBarcodeValue") val roomBarcode: String?,
    @ColumnInfo(name = "technicianName") val technicianName: String?
)
