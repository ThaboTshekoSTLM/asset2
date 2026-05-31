package za.gov.municipal.ictasset.di

import android.content.Context
import za.gov.municipal.ictasset.data.local.AppDatabase
import za.gov.municipal.ictasset.data.local.LocalDataSeeder
import za.gov.municipal.ictasset.data.repository.OfflineAssetRepository
import za.gov.municipal.ictasset.data.repository.OfflineAuthRepository
import za.gov.municipal.ictasset.data.repository.OfflineReferenceRepository
import za.gov.municipal.ictasset.data.repository.OfflineReportRepository
import za.gov.municipal.ictasset.data.repository.ReportExporter
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

    val sessionManager = SessionManager()

    val authRepository: AuthRepository = OfflineAuthRepository(
        userDao = database.userDao(),
        auditLogDao = database.auditLogDao()
    )

    val referenceRepository: ReferenceRepository = OfflineReferenceRepository(
        referenceDao = database.referenceDao()
    )

    val assetRepository: AssetRepository = OfflineAssetRepository(
        database = database,
        assetDao = database.assetDao(),
        movementDao = database.movementDao(),
        auditLogDao = database.auditLogDao(),
        seeder = seeder
    )

    val reportRepository: ReportRepository = OfflineReportRepository(
        referenceDao = database.referenceDao(),
        reportDao = database.reportDao(),
        auditLogDao = database.auditLogDao(),
        reportExporter = ReportExporter(context.applicationContext)
    )

    val registerAssetUseCase = RegisterAssetUseCase(assetRepository)
    val moveAssetUseCase = MoveAssetUseCase(assetRepository)
    val buildReportsUseCase = BuildReportsUseCase(reportRepository)

    val viewModelFactory = AppViewModelFactory(this)
}
