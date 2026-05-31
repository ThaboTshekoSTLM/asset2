package za.gov.municipal.ictasset.presentation.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import za.gov.municipal.ictasset.domain.model.User

class SessionManager {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    fun signIn(user: User) {
        _currentUser.value = user
    }

    fun signOut() {
        _currentUser.value = null
    }
}
