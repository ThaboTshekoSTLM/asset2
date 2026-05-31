package za.gov.municipal.ictasset.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import za.gov.municipal.ictasset.domain.model.AssetMovement
import za.gov.municipal.ictasset.domain.model.DashboardSummary
import za.gov.municipal.ictasset.domain.model.User
import za.gov.municipal.ictasset.domain.util.DateText
import za.gov.municipal.ictasset.presentation.components.MetricCard
import za.gov.municipal.ictasset.presentation.components.SectionTitle
import za.gov.municipal.ictasset.presentation.components.TwoColumnMetrics

@Composable
fun DashboardScreen(
    user: User,
    summary: DashboardSummary,
    recentMovements: List<AssetMovement>,
    onRegister: () -> Unit,
    onMove: () -> Unit,
    onSearch: () -> Unit,
    onReports: () -> Unit,
    onOpenStatus: (DashboardStatusType) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Welcome, ${user.fullName}",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = user.role.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        item {
            TwoColumnMetrics(
                first = { modifier ->
                    MetricCard(
                        label = "Total assets",
                        value = summary.totalAssets,
                        icon = Icons.Default.Inventory2,
                        modifier = modifier,
                        onClick = { onOpenStatus(DashboardStatusType.TOTAL_ASSETS) }
                    )
                },
                second = { modifier ->
                    MetricCard(
                        label = "Moved assets",
                        value = summary.movedAssets,
                        icon = Icons.Default.SyncAlt,
                        modifier = modifier,
                        onClick = { onOpenStatus(DashboardStatusType.MOVED_ASSETS) }
                    )
                }
            )
        }
        item {
            TwoColumnMetrics(
                first = { modifier ->
                    MetricCard(
                        label = "Allocated",
                        value = summary.allocatedAssets,
                        icon = Icons.Default.AssignmentTurnedIn,
                        modifier = modifier,
                        onClick = { onOpenStatus(DashboardStatusType.ALLOCATED_ASSETS) }
                    )
                },
                second = { modifier ->
                    MetricCard(
                        label = "Recent moves",
                        value = recentMovements.size,
                        icon = Icons.Default.History,
                        modifier = modifier,
                        onClick = { onOpenStatus(DashboardStatusType.RECENT_MOVES) }
                    )
                }
            )
        }
        item {
            SectionTitle("Field actions")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onRegister,
                        enabled = user.role.canWriteAssets,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Register asset")
                    }
                    Button(
                        onClick = onMove,
                        enabled = user.role.canWriteAssets,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Move asset")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onSearch, modifier = Modifier.weight(1f)) {
                        Text("Search assets")
                    }
                    OutlinedButton(onClick = onReports, modifier = Modifier.weight(1f)) {
                        Text("Reports")
                    }
                }
            }
        }
        item {
            SectionTitle("Recent movements")
        }
        if (recentMovements.isEmpty()) {
            item {
                Text("No movements recorded yet.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            items(recentMovements, key = { it.id }) { movement ->
                MovementCard(movement)
            }
        }
    }
}

@Composable
private fun MovementCard(movement: AssetMovement) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "${movement.assetBarcode} - ${movement.deviceDescription}",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "${movement.previousOwner} to ${movement.newOwner}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.padding(2.dp))
            Text(
                text = "${movement.movementType.displayName} | ${DateText.dateTime(movement.movementDate)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
