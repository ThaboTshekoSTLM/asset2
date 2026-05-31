package za.gov.municipal.ictasset.domain.model

enum class MovementType(val displayName: String) {
    NEW_ALLOCATION("New allocation"),
    TRANSFER("Transfer"),
    RETURN("Return"),
    REPAIR("Repair"),
    DISPOSAL("Disposal");

    companion object {
        fun fromDisplayName(value: String): MovementType =
            entries.firstOrNull { it.displayName == value } ?: TRANSFER
    }
}
