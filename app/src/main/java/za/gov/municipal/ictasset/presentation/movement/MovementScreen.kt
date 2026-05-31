package za.gov.municipal.ictasset.presentation.movement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import za.gov.municipal.ictasset.domain.model.Asset
import za.gov.municipal.ictasset.domain.model.AssetMovement
import za.gov.municipal.ictasset.domain.model.MovementType
import za.gov.municipal.ictasset.domain.model.ReferenceData
import za.gov.municipal.ictasset.domain.model.User
import za.gov.municipal.ictasset.domain.util.DateText
import za.gov.municipal.ictasset.presentation.components.AppDropdown
import za.gov.municipal.ictasset.presentation.components.AppTextField
import za.gov.municipal.ictasset.presentation.components.AssetPhotoCaptureField
import za.gov.municipal.ictasset.presentation.components.AssetPhotoPreview
import za.gov.municipal.ictasset.presentation.components.BarcodeField
import za.gov.municipal.ictasset.presentation.components.DateField
import za.gov.municipal.ictasset.presentation.components.FormColumn
import za.gov.municipal.ictasset.presentation.components.MessageBanner
import za.gov.municipal.ictasset.presentation.components.ScanButton
import za.gov.municipal.ictasset.presentation.components.SectionTitle

@Composable
fun MovementScreen(
    user: User,
    state: MovementFormState,
    referenceData: ReferenceData,
    history: List<AssetMovement>,
    onUpdate: ((MovementFormState) -> MovementFormState) -> Unit,
    onLoadAsset: () -> Unit,
    onSave: () -> Unit,
    onScanAsset: () -> Unit,
    onScanRoom: () -> Unit
) {
    if (!user.role.canWriteAssets) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Read-only access",
                style = MaterialTheme.typography.titleLarge
            )
            Text("Viewer / Auditor users cannot move or allocate assets.")
        }
        return
    }

    val selectedDepartment = referenceData.departments.firstOrNull { it.id == state.departmentId }
    val selectedBuilding = referenceData.buildings.firstOrNull { it.id == state.newBuildingId }
    val rooms = if (state.newBuildingId == null) {
        referenceData.rooms
    } else {
        referenceData.rooms.filter { it.buildingId == state.newBuildingId }
    }
    val selectedRoom = referenceData.rooms.firstOrNull { it.id == state.roomId }

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FormColumn {
                MessageBanner(message = state.message)
                SectionTitle("Find asset")
                BarcodeField(
                    value = state.assetBarcode,
                    label = "Asset barcode",
                    onValueChange = { value -> onUpdate { it.copy(assetBarcode = value.uppercase()) } },
                    onScan = onScanAsset
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = onLoadAsset, modifier = Modifier.weight(1f)) {
                        Text("Load asset")
                    }
                    ScanButton(
                        text = "Scan asset",
                        onClick = onScanAsset,
                        modifier = Modifier.weight(1f)
                    )
                }
                AssetSummary(state.selectedAsset)
                AssetPhotoCaptureField(
                    photoPath = state.assetPhotoPath,
                    onPhotoCaptured = { path -> onUpdate { it.copy(assetPhotoPath = path) } },
                    label = "Current asset photo"
                )

                SectionTitle("Move or allocate")
                AppTextField(
                    value = state.newOwner,
                    label = "New/current owner",
                    onValueChange = { value -> onUpdate { it.copy(newOwner = value) } }
                )
                AppDropdown(
                    label = "New building name",
                    selected = selectedBuilding,
                    options = referenceData.buildings,
                    optionLabel = { it.name },
                    onSelected = { building -> onUpdate { it.copy(newBuildingId = building.id, roomId = null, roomBarcode = "") } }
                )
                AppTextField(
                    value = state.newOfficeNumber,
                    label = "New office number",
                    onValueChange = { value -> onUpdate { it.copy(newOfficeNumber = value) } }
                )
                BarcodeField(
                    value = state.roomBarcode,
                    label = "Room barcode",
                    onValueChange = { value -> onUpdate { it.copy(roomBarcode = value.uppercase()) } },
                    onScan = onScanRoom
                )
                AppDropdown(
                    label = "Known room",
                    selected = selectedRoom,
                    options = rooms,
                    optionLabel = { "${it.officeNumber} / ${it.roomBarcode}" },
                    onSelected = { room ->
                        onUpdate {
                            it.copy(
                                roomId = room.id,
                                roomBarcode = room.roomBarcode,
                                newOfficeNumber = room.officeNumber,
                                newBuildingId = room.buildingId
                            )
                        }
                    }
                )
                AppDropdown(
                    label = "Department",
                    selected = selectedDepartment,
                    options = referenceData.departments,
                    optionLabel = { "${it.name} / ${it.section}" },
                    onSelected = { department ->
                        onUpdate {
                            it.copy(
                                departmentId = department.id,
                                section = department.section
                            )
                        }
                    }
                )
                AppTextField(
                    value = state.section,
                    label = "Section",
                    onValueChange = { value -> onUpdate { it.copy(section = value) } }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    DateField(
                        label = "Movement date",
                        valueMillis = state.movementDate,
                        onValueChange = { value -> onUpdate { it.copy(movementDate = value) } },
                        modifier = Modifier.weight(1f)
                    )
                    AppDropdown(
                        label = "Movement type",
                        selected = state.movementType,
                        options = MovementType.entries,
                        optionLabel = { it.displayName },
                        onSelected = { type -> onUpdate { it.copy(movementType = type) } },
                        modifier = Modifier.weight(1f)
                    )
                }
                AppTextField(
                    value = state.reason,
                    label = "Reason for movement",
                    onValueChange = { value -> onUpdate { it.copy(reason = value) } },
                    minLines = 3
                )
                AppTextField(
                    value = state.signatureConfirmation,
                    label = "Signature or confirmation",
                    onValueChange = { value -> onUpdate { it.copy(signatureConfirmation = value) } }
                )
                AppTextField(
                    value = user.fullName,
                    label = "Technician moving / allocating asset",
                    onValueChange = {},
                    readOnly = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ScanButton(
                        text = "Scan room",
                        onClick = onScanRoom,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = onSave,
                        enabled = !state.saving && state.selectedAsset != null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Text(
                            text = if (state.saving) "Saving..." else "Record",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                SectionTitle("Movement history")
            }
        }
        if (history.isEmpty()) {
            item {
                Text(
                    text = "No movement history loaded.",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        } else {
            items(history, key = { it.id }) { movement ->
                MovementHistoryCard(movement)
            }
        }
    }
}

@Composable
private fun AssetSummary(asset: Asset?) {
    if (asset == null) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(asset.deviceDescription, style = MaterialTheme.typography.titleSmall)
            Text("Barcode: ${asset.assetBarcode} | Serial: ${asset.serialNumber}")
            Text("Owner: ${asset.currentOwner}")
            Text("Location: ${asset.buildingName.orEmpty()} ${asset.officeNumber}")
            AssetPhotoPreview(photoPath = asset.assetPhotoPath)
        }
    }
}

@Composable
private fun MovementHistoryCard(movement: AssetMovement) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = movement.movementType.displayName,
                style = MaterialTheme.typography.titleSmall
            )
            Text("${movement.previousOwner} to ${movement.newOwner}")
            Text("New location: ${movement.newBuildingName.orEmpty()} ${movement.newOfficeNumber}")
            Text("Reason: ${movement.reason}")
            Text(
                text = DateText.dateTime(movement.movementDate),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
