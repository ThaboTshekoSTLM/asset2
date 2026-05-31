package za.gov.municipal.ictasset.domain.model

enum class UserRole(val displayName: String) {
    ADMIN("Admin"),
    STANDARD_USER("Standard User"),
    ICT_TECHNICIAN("ICT Technician"),
    VIEWER_AUDITOR("Viewer / Auditor");

    val canWriteAssets: Boolean
        get() = this == ADMIN || this == STANDARD_USER || this == ICT_TECHNICIAN

    val canViewReports: Boolean
        get() = true

    val canManageUsers: Boolean
        get() = this == ADMIN

    companion object {
        fun fromDisplayName(value: String): UserRole =
            entries.firstOrNull { it.displayName == value } ?: VIEWER_AUDITOR
    }
}
