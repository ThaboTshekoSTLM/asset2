package za.gov.municipal.ictasset.presentation.registration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import za.gov.municipal.ictasset.domain.model.MovementType
import za.gov.municipal.ictasset.domain.model.ReferenceData
import za.gov.municipal.ictasset.domain.model.User
import za.gov.municipal.ictasset.presentation.components.AppDropdown
import za.gov.municipal.ictasset.presentation.components.AppTextField
import za.gov.municipal.ictasset.presentation.components.AssetPhotoCaptureField
import za.gov.municipal.ictasset.presentation.components.BarcodeField
import za.gov.municipal.ictasset.presentation.components.DateField
import za.gov.municipal.ictasset.presentation.components.FormColumn
import za.gov.municipal.ictasset.presentation.components.MessageBanner
import za.gov.municipal.ictasset.presentation.components.ScanButton
import za.gov.municipal.ictasset.presentation.components.SectionTitle

@Composable
fun RegistrationScreen(
    user: User,
    state: RegistrationFormState,
    referenceData: ReferenceData,
    onUpdate: ((RegistrationFormState) -> RegistrationFormState) -> Unit,
    onSave: () -> Unit,
    onScanAsset: () -> Unit,
    onScanSerial: () -> Unit,
    onScanRoom: () -> Unit
) {
    if (!user.role.canWriteAssets) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Read-only access",
                style = MaterialTheme.typography.titleLarge
            )
            Text("Viewer / Auditor users cannot register assets.")
        }
        return
    }

    val selectedDepartment = referenceData.departments.firstOrNull { it.id == state.departmentId }
    val selectedBuilding = referenceData.buildings.firstOrNull { it.id == state.buildingId }
    val rooms = if (state.buildingId == null) {
        referenceData.rooms
    } else {
        referenceData.rooms.filter { it.buildingId == state.buildingId }
    }
    val selectedRoom = referenceData.rooms.firstOrNull { it.id == state.roomId }

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
    ) {
        item {
            FormColumn {
                MessageBanner(message = state.message)
                SectionTitle("Asset details")
                AppTextField(
                    value = state.deviceDescription,
                    label = "Device description",
                    onValueChange = { value -> onUpdate { it.copy(deviceDescription = value) } }
                )
                BarcodeField(
                    value = state.assetBarcode,
                    label = "Asset barcode",
                    onValueChange = { value -> onUpdate { it.copy(assetBarcode = value.uppercase()) } },
                    onScan = onScanAsset
                )
                BarcodeField(
                    value = state.serialNumber,
                    label = "Serial number",
                    onValueChange = { value -> onUpdate { it.copy(serialNumber = value.uppercase()) } },
                    onScan = onScanSerial
                )
                AssetPhotoCaptureField(
                    photoPath = state.assetPhotoPath,
                    onPhotoCaptured = { path -> onUpdate { it.copy(assetPhotoPath = path) } }
                )

                SectionTitle("Location and ownership")
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
                AppDropdown(
                    label = "Building name",
                    selected = selectedBuilding,
                    options = referenceData.buildings,
                    optionLabel = { it.name },
                    onSelected = { building -> onUpdate { it.copy(buildingId = building.id, roomId = null, roomBarcode = "") } }
                )
                AppTextField(
                    value = state.officeNumber,
                    label = "Office number",
                    onValueChange = { value -> onUpdate { it.copy(officeNumber = value) } }
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
                                officeNumber = room.officeNumber,
                                buildingId = room.buildingId
                            )
                        }
                    }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    AppTextField(
                        value = state.currentOwner,
                        label = "Current asset owner",
                        onValueChange = { value -> onUpdate { it.copy(currentOwner = value) } },
                        modifier = Modifier.weight(1f)
                    )
                    AppTextField(
                        value = state.previousOwner,
                        label = "Previous owner",
                        onValueChange = { value -> onUpdate { it.copy(previousOwner = value) } },
                        modifier = Modifier.weight(1f)
                    )
                }

                SectionTitle("Registration")
                AppTextField(
                    value = user.fullName,
                    label = "Technician responsible",
                    onValueChange = {},
                    readOnly = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    DateField(
                        label = "Registered date",
                        valueMillis = state.dateRegistered,
                        onValueChange = { value -> onUpdate { it.copy(dateRegistered = value) } },
                        modifier = Modifier.weight(1f)
                    )
                    DateField(
                        label = "Moved date",
                        valueMillis = state.dateMoved,
                        onValueChange = { value -> onUpdate { it.copy(dateMoved = value) } },
                        modifier = Modifier.weight(1f)
                    )
                }
                AppDropdown(
                    label = "Movement type",
                    selected = state.movementType,
                    options = MovementType.entries,
                    optionLabel = { it.displayName },
                    onSelected = { type -> onUpdate { it.copy(movementType = type) } }
                )
                AppTextField(
                    value = state.notes,
                    label = "Notes / comments",
                    onValueChange = { value -> onUpdate { it.copy(notes = value) } },
                    minLines = 3
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ScanButton(
                        text = "Scan asset",
                        onClick = onScanAsset,
                        modifier = Modifier.weight(1f)
                    )
                    ScanButton(
                        text = "Scan room",
                        onClick = onScanRoom,
                        modifier = Modifier.weight(1f)
                    )
                }
                Button(
                    onClick = onSave,
                    enabled = !state.saving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Text(
                        text = if (state.saving) "Saving..." else "Register asset",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}
