package za.gov.municipal.ictasset.domain.model

data class User(
    val id: Long,
    val fullName: String,
    val username: String,
    val role: UserRole,
    val active: Boolean
)
