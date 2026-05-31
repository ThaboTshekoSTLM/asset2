package za.gov.municipal.ictasset.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import za.gov.municipal.ictasset.di.AppContainer
import za.gov.municipal.ictasset.domain.model.ExportFormat
import za.gov.municipal.ictasset.presentation.barcode.BarcodeScannerScreen
import za.gov.municipal.ictasset.presentation.components.AssetBottomBar
import za.gov.municipal.ictasset.presentation.dashboard.DashboardScreen
import za.gov.municipal.ictasset.presentation.dashboard.DashboardStatusScreen
import za.gov.municipal.ictasset.presentation.dashboard.DashboardStatusType
import za.gov.municipal.ictasset.presentation.dashboard.DashboardStatusViewModel
import za.gov.municipal.ictasset.presentation.dashboard.DashboardViewModel
import za.gov.municipal.ictasset.presentation.login.LoginScreen
import za.gov.municipal.ictasset.presentation.login.LoginViewModel
import za.gov.municipal.ictasset.presentation.movement.MovementScreen
import za.gov.municipal.ictasset.presentation.movement.MovementViewModel
import za.gov.municipal.ictasset.presentation.registration.RegistrationScreen
import za.gov.municipal.ictasset.presentation.registration.RegistrationViewModel
import za.gov.municipal.ictasset.presentation.reports.ReportsScreen
import za.gov.municipal.ictasset.presentation.reports.ReportsViewModel
import za.gov.municipal.ictasset.presentation.search.AssetHistoryScreen
import za.gov.municipal.ictasset.presentation.search.AssetHistoryViewModel
import za.gov.municipal.ictasset.presentation.search.SearchScreen
import za.gov.municipal.ictasset.presentation.search.SearchViewModel
import za.gov.municipal.ictasset.presentation.users.UserManagementScreen
import za.gov.municipal.ictasset.presentation.users.UserManagementViewModel

@Composable
fun AssetApp(container: AppContainer) {
    LaunchedEffect(Unit) {
        container.assetRepository.seedIfNeeded()
    }
    val user by container.sessionManager.currentUser.collectAsStateWithLifecycle()
    if (user == null) {
        val loginViewModel: LoginViewModel = viewModel(factory = container.viewModelFactory)
        val state by loginViewModel.uiState.collectAsStateWithLifecycle()
        LoginScreen(
            state = state,
            onUsernameChange = loginViewModel::updateUsername,
            onPasswordChange = loginViewModel::updatePassword,
            onLogin = loginViewModel::login
        )
    } else {
        AssetNavHost(container = container)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssetNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val user by container.sessionManager.currentUser.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val fullScreen = currentRoute == Routes.HISTORY || currentRoute == Routes.SCANNER

    Scaffold(
        topBar = {
            if (!fullScreen) {
                CenterAlignedTopAppBar(
                    title = { Text(routeTitle(currentRoute)) },
                    navigationIcon = {
                        if (currentRoute == Routes.DASHBOARD_STATUS) {
                            IconButton(
                                onClick = {
                                    navController.popBackStack(Routes.DASHBOARD, inclusive = false)
                                }
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back to dashboard")
                            }
                        }
                    },
                    actions = {
                        Text(
                            text = user?.role?.displayName.orEmpty(),
                            style = MaterialTheme.typography.labelSmall
                        )
                        IconButton(onClick = { container.sessionManager.signOut() }) {
                            Icon(Icons.Default.Logout, contentDescription = "Sign out")
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (!fullScreen) {
                AssetBottomBar(
                    currentRoute = currentRoute,
                    includeUsers = user?.role?.canManageUsers == true,
                    onNavigate = { route -> navController.navigateSingleTop(route) }
                )
            }
        }
    ) { padding ->
        val contentModifier = if (fullScreen) Modifier else Modifier.padding(padding)
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = contentModifier
        ) {
            composable(Routes.DASHBOARD) {
                val viewModel: DashboardViewModel = viewModel(factory = container.viewModelFactory)
                val summary by viewModel.summary.collectAsStateWithLifecycle()
                val recent by viewModel.recentMovements.collectAsStateWithLifecycle()
                user?.let { signedIn ->
                    DashboardScreen(
                        user = signedIn,
                        summary = summary,
                        recentMovements = recent,
                        onRegister = { navController.navigateSingleTop(Routes.REGISTER) },
                        onMove = { navController.navigateSingleTop(Routes.MOVEMENT) },
                        onSearch = { navController.navigateSingleTop(Routes.SEARCH) },
                        onReports = { navController.navigateSingleTop(Routes.REPORTS) },
                        onOpenStatus = { type ->
                            navController.navigate(Routes.dashboardStatus(type.routeKey))
                        }
                    )
                }
            }
            composable(
                route = Routes.DASHBOARD_STATUS,
                arguments = listOf(navArgument("type") { type = NavType.StringType })
            ) { entry ->
                val viewModel: DashboardStatusViewModel = viewModel(factory = container.viewModelFactory)
                val assets by viewModel.assets.collectAsStateWithLifecycle()
                val movements by viewModel.movements.collectAsStateWithLifecycle()
                val type = DashboardStatusType.fromRouteKey(entry.arguments?.getString("type").orEmpty())
                DashboardStatusScreen(
                    type = type,
                    assets = assets,
                    movements = movements,
                    onBackToDashboard = {
                        navController.popBackStack(Routes.DASHBOARD, inclusive = false)
                    },
                    onOpenAssetHistory = { assetId -> navController.navigate(Routes.history(assetId)) }
                )
            }
            composable(Routes.SEARCH) { entry ->
                val viewModel: SearchViewModel = viewModel(factory = container.viewModelFactory)
                val query by viewModel.query.collectAsStateWithLifecycle()
                val assets by viewModel.assets.collectAsStateWithLifecycle()
                val scanned by entry.savedStateHandle
                    .getStateFlow(ScanKeys.SEARCH_ASSET, "")
                    .collectAsStateWithLifecycle()
                LaunchedEffect(scanned) {
                    if (scanned.isNotBlank()) {
                        viewModel.updateQuery(scanned)
                        entry.savedStateHandle[ScanKeys.SEARCH_ASSET] = ""
                    }
                }
                SearchScreen(
                    query = query,
                    assets = assets,
                    onQueryChange = viewModel::updateQuery,
                    onScan = { navController.navigate(Routes.scanner(ScanKeys.SEARCH_ASSET)) },
                    onOpenHistory = { assetId -> navController.navigate(Routes.history(assetId)) }
                )
            }
            composable(Routes.REGISTER) { entry ->
                val viewModel: RegistrationViewModel = viewModel(factory = container.viewModelFactory)
                val state by viewModel.formState.collectAsStateWithLifecycle()
                val references by viewModel.referenceData.collectAsStateWithLifecycle()
                val assetScan by entry.savedStateHandle
                    .getStateFlow(ScanKeys.REGISTER_ASSET, "")
                    .collectAsStateWithLifecycle()
                val roomScan by entry.savedStateHandle
                    .getStateFlow(ScanKeys.REGISTER_ROOM, "")
                    .collectAsStateWithLifecycle()
                LaunchedEffect(assetScan) {
                    if (assetScan.isNotBlank()) {
                        viewModel.setAssetBarcode(assetScan)
                        entry.savedStateHandle[ScanKeys.REGISTER_ASSET] = ""
                    }
                }
                LaunchedEffect(roomScan) {
                    if (roomScan.isNotBlank()) {
                        viewModel.applyRoomBarcode(roomScan)
                        entry.savedStateHandle[ScanKeys.REGISTER_ROOM] = ""
                    }
                }
                user?.let { signedIn ->
                    RegistrationScreen(
                        user = signedIn,
                        state = state,
                        referenceData = references,
                        onUpdate = viewModel::update,
                        onSave = { viewModel.save(signedIn) },
                        onScanAsset = { navController.navigate(Routes.scanner(ScanKeys.REGISTER_ASSET)) },
                        onScanRoom = { navController.navigate(Routes.scanner(ScanKeys.REGISTER_ROOM)) }
                    )
                }
            }
            composable(Routes.MOVEMENT) { entry ->
                val viewModel: MovementViewModel = viewModel(factory = container.viewModelFactory)
                val state by viewModel.formState.collectAsStateWithLifecycle()
                val references by viewModel.referenceData.collectAsStateWithLifecycle()
                val history by viewModel.history.collectAsStateWithLifecycle()
                val assetScan by entry.savedStateHandle
                    .getStateFlow(ScanKeys.MOVE_ASSET, "")
                    .collectAsStateWithLifecycle()
                val roomScan by entry.savedStateHandle
                    .getStateFlow(ScanKeys.MOVE_ROOM, "")
                    .collectAsStateWithLifecycle()
                LaunchedEffect(assetScan) {
                    if (assetScan.isNotBlank()) {
                        viewModel.setAssetBarcode(assetScan)
                        entry.savedStateHandle[ScanKeys.MOVE_ASSET] = ""
                    }
                }
                LaunchedEffect(roomScan) {
                    if (roomScan.isNotBlank()) {
                        viewModel.applyRoomBarcode(roomScan)
                        entry.savedStateHandle[ScanKeys.MOVE_ROOM] = ""
                    }
                }
                user?.let { signedIn ->
                    MovementScreen(
                        user = signedIn,
                        state = state,
                        referenceData = references,
                        history = history,
                        onUpdate = viewModel::update,
                        onLoadAsset = { viewModel.loadAsset() },
                        onSave = { viewModel.save(signedIn) },
                        onScanAsset = { navController.navigate(Routes.scanner(ScanKeys.MOVE_ASSET)) },
                        onScanRoom = { navController.navigate(Routes.scanner(ScanKeys.MOVE_ROOM)) }
                    )
                }
            }
            composable(Routes.REPORTS) {
                val viewModel: ReportsViewModel = viewModel(factory = container.viewModelFactory)
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                user?.let { signedIn ->
                    ReportsScreen(
                        state = state,
                        onSelectType = viewModel::selectType,
                        onOwnerFilterChange = viewModel::updateOwnerFilter,
                        onTechnicianFilterChange = viewModel::updateTechnicianFilter,
                        onStartDateChange = viewModel::updateStartDate,
                        onEndDateChange = viewModel::updateEndDate,
                        onGenerate = viewModel::generate,
                        onExportPdf = { viewModel.export(ExportFormat.PDF, signedIn) },
                        onExportExcel = { viewModel.export(ExportFormat.EXCEL, signedIn) }
                    )
                }
            }
            composable(Routes.USERS) {
                val viewModel: UserManagementViewModel = viewModel(factory = container.viewModelFactory)
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val users by viewModel.users.collectAsStateWithLifecycle()
                user?.let { signedIn ->
                    UserManagementScreen(
                        actor = signedIn,
                        state = state,
                        users = users,
                        onFullNameChange = viewModel::updateFullName,
                        onUsernameChange = viewModel::updateUsername,
                        onPasswordChange = viewModel::updatePassword,
                        onRoleChange = viewModel::updateRole,
                        onCreateUser = { viewModel.createUser(signedIn) },
                        onDeleteUser = { userId -> viewModel.deleteUser(userId, signedIn) }
                    )
                }
            }
            composable(
                route = Routes.HISTORY,
                arguments = listOf(navArgument("assetId") { type = NavType.LongType })
            ) { entry ->
                val viewModel: AssetHistoryViewModel = viewModel(factory = container.viewModelFactory)
                val assetId = entry.arguments?.getLong("assetId") ?: 0L
                val asset by viewModel.asset.collectAsStateWithLifecycle()
                val movements by viewModel.movements.collectAsStateWithLifecycle()
                LaunchedEffect(assetId) {
                    if (assetId > 0) viewModel.load(assetId)
                }
                AssetHistoryScreen(
                    asset = asset,
                    movements = movements,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Routes.SCANNER,
                arguments = listOf(navArgument("target") { type = NavType.StringType })
            ) { entry ->
                val target = entry.arguments?.getString("target").orEmpty()
                BarcodeScannerScreen(
                    title = scannerTitle(target),
                    onBarcodeScanned = { value ->
                        navController.previousBackStackEntry?.savedStateHandle?.set(target, value)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

private fun NavHostController.navigateSingleTop(route: String) {
    navigate(route) {
        launchSingleTop = true
        restoreState = true
        popUpTo(Routes.DASHBOARD) {
            saveState = true
        }
    }
}

private fun routeTitle(route: String?): String =
    when (route) {
        Routes.SEARCH -> "Search Assets"
        Routes.REGISTER -> "Register Asset"
        Routes.MOVEMENT -> "Move Asset"
        Routes.REPORTS -> "Reports"
        Routes.USERS -> "Users"
        Routes.DASHBOARD_STATUS -> "Dashboard Status"
        else -> "ICT Asset Register"
    }

private fun scannerTitle(target: String): String =
    when (target) {
        ScanKeys.REGISTER_ROOM, ScanKeys.MOVE_ROOM -> "Scan room barcode"
        else -> "Scan asset barcode"
    }
