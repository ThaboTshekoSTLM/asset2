package za.gov.municipal.ictasset.data.repository

import android.database.sqlite.SQLiteConstraintException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import za.gov.municipal.ictasset.data.local.PasswordHasher
import za.gov.municipal.ictasset.data.local.dao.AuditLogDao
import za.gov.municipal.ictasset.data.local.dao.UserDao
import za.gov.municipal.ictasset.data.local.entity.AuditLogEntity
import za.gov.municipal.ictasset.data.local.entity.UserEntity
import za.gov.municipal.ictasset.domain.model.AuditAction
import za.gov.municipal.ictasset.domain.model.CreateUserRequest
import za.gov.municipal.ictasset.domain.model.SaveResult
import za.gov.municipal.ictasset.domain.model.User
import za.gov.municipal.ictasset.domain.repository.AuthRepository

class OfflineAuthRepository(
    private val userDao: UserDao,
    private val auditLogDao: AuditLogDao
) : AuthRepository {
    override suspend fun login(username: String, password: String): User? {
        val user = userDao.findByUsername(username.trim()) ?: return null
        if (!user.active || user.passwordHash != PasswordHasher.hash(password)) return null

        auditLogDao.insert(
            AuditLogEntity(
                actorUserId = user.id,
                action = AuditAction.LOGIN,
                entityType = "user",
                entityId = user.id,
                details = "User logged in offline."
            )
        )
        return user.toDomain()
    }

    override suspend fun findUser(id: Long): User? =
        userDao.findById(id)?.toDomain()

    override fun observeUsers(): Flow<List<User>> =
        userDao.observeActiveUsers().map { users -> users.map { it.toDomain() } }

    override suspend fun createUser(request: CreateUserRequest, actor: User): SaveResult {
        if (!actor.role.canManageUsers) {
            return SaveResult.Error("Only admin users can create users.")
        }
        val fullName = request.fullName.trim()
        val username = request.username.trim().lowercase()
        val password = request.password.trim()
        val validationError = when {
            fullName.isBlank() -> "Full name is required."
            username.isBlank() -> "Username is required."
            password.length < 4 -> "Password must be at least 4 characters."
            else -> null
        }
        if (validationError != null) return SaveResult.Error(validationError)

        return try {
            val userId = userDao.insert(
                UserEntity(
                    fullName = fullName,
                    username = username,
                    passwordHash = PasswordHasher.hash(password),
                    role = request.role
                )
            )
            auditLogDao.insert(
                AuditLogEntity(
                    actorUserId = actor.id,
                    action = AuditAction.CREATE_USER,
                    entityType = "user",
                    entityId = userId,
                    details = "Created user $username with role ${request.role.displayName}."
                )
            )
            SaveResult.Success(userId)
        } catch (exception: SQLiteConstraintException) {
            SaveResult.Error("Username already exists.")
        }
    }

    override suspend fun deleteUser(userId: Long, actor: User): SaveResult {
        if (!actor.role.canManageUsers) {
            return SaveResult.Error("Only admin users can delete users.")
        }
        if (userId == actor.id) {
            return SaveResult.Error("You cannot delete your own admin account while signed in.")
        }
        val user = userDao.findById(userId) ?: return SaveResult.Error("User not found.")
        if (!user.active) return SaveResult.Error("User is already deleted.")
        if (user.role.canManageUsers && userDao.activeAdminCount() <= 1) {
            return SaveResult.Error("At least one active admin account is required.")
        }

        userDao.deactivate(userId)
        auditLogDao.insert(
            AuditLogEntity(
                actorUserId = actor.id,
                action = AuditAction.DELETE_USER,
                entityType = "user",
                entityId = userId,
                details = "Deleted user ${user.username}."
            )
        )
        return SaveResult.Success(userId)
    }
}
