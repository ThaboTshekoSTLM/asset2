package za.gov.municipal.ictasset.presentation.users

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import za.gov.municipal.ictasset.domain.model.User
import za.gov.municipal.ictasset.domain.model.UserRole
import za.gov.municipal.ictasset.presentation.components.AppDropdown
import za.gov.municipal.ictasset.presentation.components.AppTextField
import za.gov.municipal.ictasset.presentation.components.MessageBanner
import za.gov.municipal.ictasset.presentation.components.SectionTitle

@Composable
fun UserManagementScreen(
    actor: User,
    state: UserManagementUiState,
    users: List<User>,
    onFullNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRoleChange: (UserRole) -> Unit,
    onCreateUser: () -> Unit,
    onDeleteUser: (Long) -> Unit
) {
    if (!actor.role.canManageUsers) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Admin access required", style = MaterialTheme.typography.titleLarge)
            Text("Only admin users can create or delete users.")
        }
        return
    }

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("User management", style = MaterialTheme.typography.titleLarge)
        }
        item {
            MessageBanner(message = state.message)
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SectionTitle("Create user")
                    AppTextField(
                        value = state.fullName,
                        label = "Full name",
                        onValueChange = onFullNameChange
                    )
                    AppTextField(
                        value = state.username,
                        label = "Username",
                        onValueChange = onUsernameChange
                    )
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = onPasswordChange,
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    AppDropdown(
                        label = "Role",
                        selected = state.selectedRole,
                        options = UserRole.entries,
                        optionLabel = { it.displayName },
                        onSelected = onRoleChange
                    )
                    Button(
                        onClick = onCreateUser,
                        enabled = !state.saving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                        Text(
                            text = if (state.saving) "Saving..." else "Create user",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
        item {
            SectionTitle("Active users")
        }
        if (users.isEmpty()) {
            item {
                Text("No active users found.")
            }
        } else {
            items(users, key = { it.id }) { user ->
                UserCard(
                    actor = actor,
                    user = user,
                    onDeleteUser = onDeleteUser
                )
            }
        }
    }
}

@Composable
private fun UserCard(
    actor: User,
    user: User,
    onDeleteUser: (Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(user.fullName, style = MaterialTheme.typography.titleSmall)
                Text("Username: ${user.username}")
                Text("Role: ${user.role.displayName}")
            }
            IconButton(
                onClick = { onDeleteUser(user.id) },
                enabled = user.id != actor.id
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete user")
            }
        }
    }
}
