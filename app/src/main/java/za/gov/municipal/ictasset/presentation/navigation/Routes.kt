package za.gov.municipal.ictasset.presentation.navigation

object Routes {
    const val DASHBOARD = "dashboard"
    const val SEARCH = "search"
    const val REGISTER = "register"
    const val MOVEMENT = "movement"
    const val REPORTS = "reports"
    const val USERS = "users"
    const val DASHBOARD_STATUS = "dashboardStatus/{type}"
    const val HISTORY = "history/{assetId}"
    const val SCANNER = "scanner/{target}"

    fun dashboardStatus(type: String): String = "dashboardStatus/$type"
    fun history(assetId: Long): String = "history/$assetId"
    fun scanner(target: String): String = "scanner/$target"
}

object ScanKeys {
    const val REGISTER_ASSET = "scan_register_asset"
    const val REGISTER_SERIAL = "scan_register_serial"
    const val REGISTER_ROOM = "scan_register_room"
    const val MOVE_ASSET = "scan_move_asset"
    const val MOVE_ROOM = "scan_move_room"
    const val SEARCH_ASSET = "scan_search_asset"
}
