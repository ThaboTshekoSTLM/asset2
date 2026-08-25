package za.gov.municipal.ictasset.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONObject
import za.gov.municipal.ictasset.data.remote.SupabaseApi
import za.gov.municipal.ictasset.domain.model.CreateUserRequest
import za.gov.municipal.ictasset.domain.model.SaveResult
import za.gov.municipal.ictasset.domain.model.User
import za.gov.municipal.ictasset.domain.model.UserRole
import za.gov.municipal.ictasset.domain.repository.AuthRepository

class SupabaseAuthRepository(
    private val api: SupabaseApi,
    private val onAuthenticated: suspend () -> Unit
) : AuthRepository {
    private val users = MutableStateFlow<List<User>>(emptyList())

    override suspend fun login(username: String, password: String): User? {
        val email = username.trim().lowercase().let {
            if ('@' in it) it else "$it@ict-register.local"
        }
        val profile = api.login(email, password)
        if (!profile.optBoolean("active", true)) {
            error("This user account is inactive.")
        }

        // Authentication has succeeded at this point. A temporary failure while
        // refreshing application data must not turn a valid login into an
        // "invalid credentials" result.
        runCatching { refreshUsers() }
        runCatching { onAuthenticated() }
        return profile.toUser()
    }

    override suspend fun findUser(id: Long): User? = users.value.firstOrNull { it.id == id }

    override fun observeUsers(): Flow<List<User>> = users

    override suspend fun createUser(request: CreateUserRequest, actor: User): SaveResult =
        SaveResult.Error("Create production users in Supabase Authentication, then assign their role in profiles.")

    override suspend fun deleteUser(userId: Long, actor: User): SaveResult =
        SaveResult.Error("Delete production users in Supabase Authentication.")

    suspend fun refreshUsers() {
        users.value = api.fetchProfiles().jsonObjects().map { it.toUser() }
    }
}

internal fun JSONObject.toUser(): User = User(
    id = stableLong(getString("id")),
    fullName = getString("full_name"),
    username = getString("username"),
    role = when (getString("role")) {
        "admin" -> UserRole.ADMIN
        "standard_user" -> UserRole.STANDARD_USER
        "ict_technician" -> UserRole.ICT_TECHNICIAN
        else -> UserRole.VIEWER_AUDITOR
    },
    active = optBoolean("active", true)
)

internal fun stableLong(value: String): Long =
    value.fold(1125899906842597L) { hash, char -> hash * 31 + char.code }.let { if (it == Long.MIN_VALUE) 1 else kotlin.math.abs(it) }

internal fun org.json.JSONArray.jsonObjects(): List<JSONObject> =
    (0 until length()).map { getJSONObject(it) }
