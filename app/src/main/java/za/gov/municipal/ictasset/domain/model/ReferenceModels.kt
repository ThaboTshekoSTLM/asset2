package za.gov.municipal.ictasset.domain.model

data class Department(
    val id: Long,
    val name: String,
    val section: String
)

data class Building(
    val id: Long,
    val name: String,
    val address: String
)

data class Room(
    val id: Long,
    val buildingId: Long,
    val buildingName: String?,
    val officeNumber: String,
    val roomBarcode: String,
    val description: String
)

data class ReferenceData(
    val departments: List<Department>,
    val buildings: List<Building>,
    val rooms: List<Room>
)
