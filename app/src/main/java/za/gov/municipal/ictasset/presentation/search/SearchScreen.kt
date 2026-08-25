package za.gov.municipal.ictasset.presentation.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import za.gov.municipal.ictasset.domain.model.Asset
import za.gov.municipal.ictasset.domain.model.AssetMovement
import za.gov.municipal.ictasset.domain.util.DateText
import za.gov.municipal.ictasset.presentation.components.AssetPhotoPreview
import za.gov.municipal.ictasset.presentation.components.BarcodeField
import za.gov.municipal.ictasset.presentation.components.SectionTitle

@Composable
fun SearchScreen(
    query: String,
    assets: List<Asset>,
    onQueryChange: (String) -> Unit,
    onScan: () -> Unit,
    onOpenHistory: (Long) -> Unit
) {
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Search assets",
                style = MaterialTheme.typography.titleLarge
            )
        }
        item {
            BarcodeField(
                value = query,
                label = "Search",
                onValueChange = onQueryChange,
                onScan = onScan
            )
        }
        if (assets.isEmpty()) {
            item {
                Text("No assets found.")
            }
        } else {
            items(assets, key = { it.id }) { asset ->
                AssetSearchCard(asset = asset, onClick = { onOpenHistory(asset.id) })
            }
        }
    }
}

@Composable
private fun AssetSearchCard(
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
            Text("Owner: ${asset.currentOwner}")
            Text("Department: ${asset.departmentName.orEmpty()} / ${asset.section}")
            Text("Building: ${asset.buildingName.orEmpty()} | Office: ${asset.officeNumber}")
            Text("Tap to view this device and its photo", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetHistoryScreen(
    asset: Asset?,
    movements: List<AssetMovement>,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Movement history") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                if (asset == null) {
                    Text("Loading asset...")
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(asset.deviceDescription, style = MaterialTheme.typography.titleMedium)
                            Text("Barcode: ${asset.assetBarcode}")
                            Text("Serial: ${asset.serialNumber}")
                            Text("Current owner: ${asset.currentOwner}")
                            Text("Registered: ${DateText.date(asset.dateRegistered)}")
                            AssetPhotoPreview(photoPath = asset.assetPhotoPath)
                        }
                    }
                }
            }
            item {
                SectionTitle("Historical movement records")
            }
            if (movements.isEmpty()) {
                item { Text("No movement records found.") }
            } else {
                items(movements, key = { it.id }) { movement ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(movement.movementType.displayName, style = MaterialTheme.typography.titleSmall)
                            Text("${movement.previousOwner} to ${movement.newOwner}")
                            Text("Previous location: ${movement.previousLocation}")
                            Text("New location: ${movement.newBuildingName.orEmpty()} ${movement.newOfficeNumber}")
                            Text("Technician: ${movement.technicianName.orEmpty()}")
                            Text("Reason: ${movement.reason}")
                            Text("Confirmation: ${movement.signatureConfirmation}")
                            Text(
                                text = DateText.dateTime(movement.movementDate),
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
