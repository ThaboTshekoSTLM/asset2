package za.gov.municipal.ictasset.presentation.dashboard

enum class DashboardStatusType(
    val routeKey: String,
    val title: String
) {
    TOTAL_ASSETS("total-assets", "Total assets"),
    MOVED_ASSETS("moved-assets", "Moved assets"),
    ALLOCATED_ASSETS("allocated-assets", "Allocated assets"),
    RECENT_MOVES("recent-moves", "Recent moves");

    companion object {
        fun fromRouteKey(value: String): DashboardStatusType =
            entries.firstOrNull { it.routeKey == value } ?: TOTAL_ASSETS
    }
}
