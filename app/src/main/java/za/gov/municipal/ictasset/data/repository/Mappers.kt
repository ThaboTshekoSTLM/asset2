package za.gov.municipal.ictasset.data.repository

import za.gov.municipal.ictasset.data.local.entity.AssetEntity
import za.gov.municipal.ictasset.data.local.entity.AssetMovementEntity
import za.gov.municipal.ictasset.data.local.entity.BuildingEntity
import za.gov.municipal.ictasset.data.local.entity.DepartmentEntity
import za.gov.municipal.ictasset.data.local.entity.RoomEntity
import za.gov.municipal.ictasset.data.local.entity.UserEntity
import za.gov.municipal.ictasset.data.local.model.AssetWithLocation
import za.gov.municipal.ictasset.data.local.model.MovementWithLocation
import za.gov.municipal.ictasset.domain.model.Asset
import za.gov.municipal.ictasset.domain.model.AssetMovement
import za.gov.municipal.ictasset.domain.model.Building
import za.gov.municipal.ictasset.domain.model.Department
import za.gov.municipal.ictasset.domain.model.Room
import za.gov.municipal.ictasset.domain.model.User

fun UserEntity.toDomain(): User =
    User(
        id = id,
        fullName = fullName,
        username = username,
        role = role,
        active = active
    )

fun DepartmentEntity.toDomain(): Department =
    Department(id = id, name = name, section = section)

fun BuildingEntity.toDomain(): Building =
    Building(id = id, name = name, address = address)

fun RoomEntity.toDomain(buildingName: String?): Room =
    Room(
        id = id,
        buildingId = buildingId,
        buildingName = buildingName,
        officeNumber = officeNumber,
        roomBarcode = roomBarcode,
        description = description
    )

fun AssetEntity.toDomain(): Asset =
    Asset(
        id = id,
        deviceDescription = deviceDescription,
        assetBarcode = assetBarcode,
        serialNumber = serialNumber,
        departmentId = departmentId,
        departmentName = null,
        section = section,
        buildingId = buildingId,
        buildingName = null,
        officeNumber = officeNumber,
        roomId = roomId,
        roomBarcode = roomBarcode,
        currentOwner = currentOwner,
        previousOwner = previousOwner,
        technicianResponsibleId = technicianResponsibleId,
        technicianResponsibleName = null,
        dateRegistered = dateRegistered,
        dateMoved = dateMoved,
        movementType = movementType,
        notes = notes,
        assetPhotoPath = assetPhotoPath.ifBlank { null }
    )

fun AssetWithLocation.toDomain(): Asset =
    Asset(
        id = asset.id,
        deviceDescription = asset.deviceDescription,
        assetBarcode = asset.assetBarcode,
        serialNumber = asset.serialNumber,
        departmentId = asset.departmentId,
        departmentName = departmentName,
        section = asset.section,
        buildingId = asset.buildingId,
        buildingName = buildingName,
        officeNumber = asset.officeNumber,
        roomId = asset.roomId,
        roomBarcode = asset.roomBarcode.ifBlank { roomBarcode.orEmpty() },
        currentOwner = asset.currentOwner,
        previousOwner = asset.previousOwner,
        technicianResponsibleId = asset.technicianResponsibleId,
        technicianResponsibleName = technicianName,
        dateRegistered = asset.dateRegistered,
        dateMoved = asset.dateMoved,
        movementType = asset.movementType,
        notes = asset.notes,
        assetPhotoPath = asset.assetPhotoPath.ifBlank { null }
    )

fun AssetMovementEntity.toDomain(): AssetMovement =
    AssetMovement(
        id = id,
        assetId = assetId,
        technicianUserId = technicianUserId,
        technicianName = null,
        previousOwner = previousOwner,
        newOwner = newOwner,
        previousLocation = previousLocation,
        newBuildingId = newBuildingId,
        newBuildingName = null,
        newOfficeNumber = newOfficeNumber,
        departmentId = departmentId,
        departmentName = null,
        section = section,
        roomId = roomId,
        roomBarcode = roomBarcode,
        deviceDescription = deviceDescription,
        assetBarcode = assetBarcode,
        serialNumber = serialNumber,
        movementDate = movementDate,
        movementType = movementType,
        reason = reason,
        signatureConfirmation = signatureConfirmation
    )

fun MovementWithLocation.toDomain(): AssetMovement =
    AssetMovement(
        id = movement.id,
        assetId = movement.assetId,
        technicianUserId = movement.technicianUserId,
        technicianName = technicianName,
        previousOwner = movement.previousOwner,
        newOwner = movement.newOwner,
        previousLocation = movement.previousLocation,
        newBuildingId = movement.newBuildingId,
        newBuildingName = buildingName,
        newOfficeNumber = movement.newOfficeNumber,
        departmentId = movement.departmentId,
        departmentName = departmentName,
        section = movement.section,
        roomId = movement.roomId,
        roomBarcode = movement.roomBarcode.ifBlank { roomBarcode.orEmpty() },
        deviceDescription = movement.deviceDescription,
        assetBarcode = movement.assetBarcode,
        serialNumber = movement.serialNumber,
        movementDate = movement.movementDate,
        movementType = movement.movementType,
        reason = movement.reason,
        signatureConfirmation = movement.signatureConfirmation
    )
