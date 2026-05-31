package za.gov.municipal.ictasset.presentation.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import za.gov.municipal.ictasset.domain.model.Asset
import za.gov.municipal.ictasset.domain.model.AssetMovement
import za.gov.municipal.ictasset.domain.util.DateText
import za.gov.municipal.ictasset.presentation.components.AssetPhotoPreview

@Composable
fun DashboardStatusScreen(
    type: DashboardStatusType,
    assets: List<Asset>,
    movements: List<AssetMovement>,
    onBackToDashboard: () -> Unit,
    onOpenAssetHistory: (Long) -> Unit
) {
    val displayedAssets = when (type) {
        DashboardStatusType.TOTAL_ASSETS -> assets
        DashboardStatusType.ALLOCATED_ASSETS -> assets.filter { it.currentOwner.isNotBlank() }
        else -> emptyList()
    }
    val displayedMovements = when (type) {
        DashboardStatusType.MOVED_ASSETS -> movements
        DashboardStatusType.RECENT_MOVES -> movements.take(10)
        else -> emptyList()
    }
    val count = displayedAssets.size + displayedMovements.size

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = type.title, style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "$count record${if (count == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                OutlinedButton(onClick = onBackToDashboard) {
                    Text("Home")
                }
            }
        }
        when (type) {
            DashboardStatusType.TOTAL_ASSETS,
            DashboardStatusType.ALLOCATED_ASSETS -> {
                if (displayedAssets.isEmpty()) {
                    item { Text("No assets found.") }
                } else {
                    items(displayedAssets, key = { it.id }) { asset ->
                        AssetStatusCard(asset = asset, onClick = { onOpenAssetHistory(asset.id) })
                    }
                }
            }

            DashboardStatusType.MOVED_ASSETS,
            DashboardStatusType.RECENT_MOVES -> {
                if (displayedMovements.isEmpty()) {
                    item { Text("No movement records found.") }
                } else {
                    items(displayedMovements, key = { it.id }) { movement ->
                        MovementStatusCard(movement)
                    }
                }
            }
        }
    }
}

@Composable
private fun AssetStatusCard(
    asset: Asset,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(asset.deviceDescription, style = MaterialTheme.typography.titleSmall)
            Text("Barcode: ${asset.assetBarcode} | Serial: ${asset.serialNumber}")
            Text("Owner: ${asset.currentOwner.ifBlank { "Unassigned" }}")
            Text("Department: ${asset.departmentName.orEmpty()} / ${asset.section}")
            Text("Building: ${asset.buildingName.orEmpty()} | Office: ${asset.officeNumber}")
            Text("Registered: ${DateText.date(asset.dateRegistered)}")
            AssetPhotoPreview(photoPath = asset.assetPhotoPath)
        }
    }
}

@Composable
private fun MovementStatusCard(movement: AssetMovement) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "${movement.assetBarcode} - ${movement.deviceDescription}",
                style = MaterialTheme.typography.titleSmall
            )
            Text("${movement.previousOwner} to ${movement.newOwner}")
            Text("Location: ${movement.newBuildingName.orEmpty()} ${movement.newOfficeNumber}")
            Text("Technician: ${movement.technicianName.orEmpty()}")
            Text("Reason: ${movement.reason}")
            Text(
                text = "${movement.movementType.displayName} | ${DateText.dateTime(movement.movementDate)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
