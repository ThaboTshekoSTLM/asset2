package za.gov.municipal.ictasset.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import za.gov.municipal.ictasset.presentation.dashboard.DashboardStatusViewModel
import za.gov.municipal.ictasset.presentation.dashboard.DashboardViewModel
import za.gov.municipal.ictasset.presentation.login.LoginViewModel
import za.gov.municipal.ictasset.presentation.movement.MovementViewModel
import za.gov.municipal.ictasset.presentation.registration.RegistrationViewModel
import za.gov.municipal.ictasset.presentation.reports.ReportsViewModel
import za.gov.municipal.ictasset.presentation.search.AssetHistoryViewModel
import za.gov.municipal.ictasset.presentation.search.SearchViewModel
import za.gov.municipal.ictasset.presentation.users.UserManagementViewModel

class AppViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) ->
                LoginViewModel(container.authRepository, container.sessionManager) as T

            modelClass.isAssignableFrom(DashboardViewModel::class.java) ->
                DashboardViewModel(container.assetRepository) as T

            modelClass.isAssignableFrom(DashboardStatusViewModel::class.java) ->
                DashboardStatusViewModel(container.assetRepository) as T

            modelClass.isAssignableFrom(RegistrationViewModel::class.java) ->
                RegistrationViewModel(
                    container.registerAssetUseCase,
                    container.referenceRepository
                ) as T

            modelClass.isAssignableFrom(MovementViewModel::class.java) ->
                MovementViewModel(
                    container.assetRepository,
                    container.referenceRepository,
                    container.moveAssetUseCase
                ) as T

            modelClass.isAssignableFrom(SearchViewModel::class.java) ->
                SearchViewModel(container.assetRepository) as T

            modelClass.isAssignableFrom(AssetHistoryViewModel::class.java) ->
                AssetHistoryViewModel(container.assetRepository) as T

            modelClass.isAssignableFrom(ReportsViewModel::class.java) ->
                ReportsViewModel(
                    container.buildReportsUseCase,
                    container.reportRepository
                ) as T

            modelClass.isAssignableFrom(UserManagementViewModel::class.java) ->
                UserManagementViewModel(container.authRepository) as T

            else -> error("Unknown ViewModel class ${modelClass.name}")
        }
}
