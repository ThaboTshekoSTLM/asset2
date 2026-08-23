package za.gov.municipal.ictasset.di

import android.content.Context
import za.gov.municipal.ictasset.BuildConfig
import za.gov.municipal.ictasset.data.local.AppDatabase
import za.gov.municipal.ictasset.data.local.LocalDataSeeder
import za.gov.municipal.ictasset.data.repository.OfflineReferenceRepository
import za.gov.municipal.ictasset.data.repository.ReportExporter
import za.gov.municipal.ictasset.data.remote.SupabaseApi
import za.gov.municipal.ictasset.data.repository.SupabaseAssetRepository
import za.gov.municipal.ictasset.data.repository.SupabaseAuthRepository
import za.gov.municipal.ictasset.data.repository.SupabaseReportRepository
import za.gov.municipal.ictasset.domain.repository.AssetRepository
import za.gov.municipal.ictasset.domain.repository.AuthRepository
import za.gov.municipal.ictasset.domain.repository.ReferenceRepository
import za.gov.municipal.ictasset.domain.repository.ReportRepository
import za.gov.municipal.ictasset.domain.usecase.BuildReportsUseCase
import za.gov.municipal.ictasset.domain.usecase.MoveAssetUseCase
import za.gov.municipal.ictasset.domain.usecase.RegisterAssetUseCase
import za.gov.municipal.ictasset.presentation.session.SessionManager

class AppContainer(context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val seeder = LocalDataSeeder(
        database = database,
        userDao = database.userDao(),
        referenceDao = database.referenceDao(),
        assetDao = database.assetDao(),
        movementDao = database.movementDao(),
        auditLogDao = database.auditLogDao()
    )

    private val supabaseApi = SupabaseApi(
        context = context.applicationContext,
        projectUrl = BuildConfig.SUPABASE_URL,
        publishableKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
    )

    val sessionManager = SessionManager(supabaseApi::signOut)

    val referenceRepository: ReferenceRepository = OfflineReferenceRepository(
        referenceDao = database.referenceDao()
    )

    private val supabaseAssetRepository = SupabaseAssetRepository(
        api = supabaseApi,
        referenceDao = database.referenceDao(),
        seeder = seeder
    )

    val assetRepository: AssetRepository = supabaseAssetRepository

    val authRepository: AuthRepository = SupabaseAuthRepository(
        api = supabaseApi,
        onAuthenticated = supabaseAssetRepository::refresh
    )

    val reportRepository: ReportRepository = SupabaseReportRepository(
        assets = supabaseAssetRepository,
        reportExporter = ReportExporter(context.applicationContext)
    )

    val registerAssetUseCase = RegisterAssetUseCase(assetRepository)
    val moveAssetUseCase = MoveAssetUseCase(assetRepository)
    val buildReportsUseCase = BuildReportsUseCase(reportRepository)

    val viewModelFactory = AppViewModelFactory(this)
}
