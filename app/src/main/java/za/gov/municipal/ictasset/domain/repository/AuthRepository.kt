package za.gov.municipal.ictasset.domain.repository

import kotlinx.coroutines.flow.Flow
import za.gov.municipal.ictasset.domain.model.CreateUserRequest
import za.gov.municipal.ictasset.domain.model.SaveResult
import za.gov.municipal.ictasset.domain.model.User

interface AuthRepository {
    suspend fun login(username: String, password: String): User?
    suspend fun findUser(id: Long): User?
    fun observeUsers(): Flow<List<User>>
    suspend fun createUser(request: CreateUserRequest, actor: User): SaveResult
    suspend fun deleteUser(userId: Long, actor: User): SaveResult
}
