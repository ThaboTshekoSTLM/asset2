package za.gov.municipal.ictasset.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.PersonPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun MetricCard(
    label: String,
    value: Int,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    if (onClick == null) {
        Card(
            modifier = modifier.aspectRatio(1.6f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            MetricCardContent(label = label, value = value, icon = icon)
        }
    } else {
        Card(
            onClick = onClick,
            modifier = modifier.aspectRatio(1.6f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            MetricCardContent(label = label, value = value, icon = icon)
        }
    }
}

@Composable
private fun MetricCardContent(
    label: String,
    value: Int,
    icon: ImageVector
) {
    Column(modifier = Modifier.padding(14.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = value.toString(), style = MaterialTheme.typography.headlineMedium)
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

data class BottomDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val MainDestinations = listOf(
    BottomDestination("dashboard", "Home", Icons.Default.Inventory2),
    BottomDestination("search", "Search", Icons.Default.Search),
    BottomDestination("movement", "Move", Icons.Default.SyncAlt),
    BottomDestination("register", "Register", Icons.Default.PersonPin),
    BottomDestination("reports", "Reports", Icons.Default.Assessment)
)

private val UserManagementDestination =
    BottomDestination("users", "Users", Icons.Default.ManageAccounts)

@Composable
fun AssetBottomBar(
    currentRoute: String?,
    includeUsers: Boolean,
    onNavigate: (String) -> Unit
) {
    val destinations = if (includeUsers) {
        MainDestinations + UserManagementDestination
    } else {
        MainDestinations
    }
    NavigationBar {
        destinations.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = { onNavigate(destination.route) },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = { Text(destination.label) }
            )
        }
    }
}

@Composable
fun TwoColumnMetrics(
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        first(Modifier.weight(1f))
        Spacer(modifier = Modifier.width(12.dp))
        second(Modifier.weight(1f))
    }
}
