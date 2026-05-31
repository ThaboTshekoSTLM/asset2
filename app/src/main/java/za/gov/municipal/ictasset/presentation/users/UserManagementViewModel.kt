package za.gov.municipal.ictasset.presentation.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import za.gov.municipal.ictasset.domain.model.CreateUserRequest
import za.gov.municipal.ictasset.domain.model.SaveResult
import za.gov.municipal.ictasset.domain.model.User
import za.gov.municipal.ictasset.domain.model.UserRole
import za.gov.municipal.ictasset.domain.repository.AuthRepository

data class UserManagementUiState(
    val fullName: String = "",
    val username: String = "",
    val password: String = "",
    val selectedRole: UserRole = UserRole.STANDARD_USER,
    val saving: Boolean = false,
    val message: String? = null
)

class UserManagementViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(UserManagementUiState())
    val uiState: StateFlow<UserManagementUiState> = _uiState.asStateFlow()

    val users: StateFlow<List<User>> =
        authRepository.observeUsers()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateFullName(value: String) {
        _uiState.update { it.copy(fullName = value, message = null) }
    }

    fun updateUsername(value: String) {
        _uiState.update { it.copy(username = value.lowercase(), message = null) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value, message = null) }
    }

    fun updateRole(value: UserRole) {
        _uiState.update { it.copy(selectedRole = value, message = null) }
    }

    fun createUser(actor: User) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true, message = null) }
            val result = authRepository.createUser(
                request = CreateUserRequest(
                    fullName = state.fullName,
                    username = state.username,
                    password = state.password,
                    role = state.selectedRole
                ),
                actor = actor
            )
            _uiState.update {
                when (result) {
                    is SaveResult.Success -> UserManagementUiState(message = "User created successfully.")
                    is SaveResult.Error -> it.copy(saving = false, message = result.message)
                }
            }
        }
    }

    fun deleteUser(userId: Long, actor: User) {
        viewModelScope.launch {
            val result = authRepository.deleteUser(userId, actor)
            _uiState.update {
                when (result) {
                    is SaveResult.Success -> it.copy(message = "User deleted.")
                    is SaveResult.Error -> it.copy(message = result.message)
                }
            }
        }
    }
}
