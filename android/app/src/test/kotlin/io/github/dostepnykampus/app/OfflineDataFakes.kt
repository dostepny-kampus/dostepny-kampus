/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import io.github.dostepnykampus.core.data.building.BuildingFloors
import io.github.dostepnykampus.core.data.network.NetworkStatus
import io.github.dostepnykampus.core.data.start.FirstLaunch
import io.github.dostepnykampus.core.data.submission.own.OwnStatuses
import io.github.dostepnykampus.core.data.submission.own.OwnSubmissions
import io.github.dostepnykampus.core.data.submission.own.StatusRefresh
import io.github.dostepnykampus.core.data.submission.pending.PendingEdits
import io.github.dostepnykampus.core.data.submission.sync.SubmissionSyncScheduler
import kotlinx.coroutines.flow.flowOf
import javax.inject.Singleton

/**
 * Wspólne źródła danych testów Hilt aplikacji bez sieci: sieć dostępna, pomoc już widziana, brak własnych
 * zgłoszeń, poprawek i planów pięter. Moduł testu dołącza je przez `includes`.
 */
@Module
@DisableInstallInCheck
object OfflineDataFakes {
    @Provides
    fun buildingFloors(): BuildingFloors = BuildingFloors { null }

    @Provides
    @Singleton
    fun network(): NetworkStatus = NetworkStatus { flowOf(true) }

    @Provides
    @Singleton
    fun firstLaunch(): FirstLaunch =
        object : FirstLaunch {
            override suspend fun isFirstLaunch(): Boolean = false

            override suspend fun markHelpSeen() = Unit
        }

    @Provides
    fun ownSubmissions(): OwnSubmissions = OwnSubmissions { flowOf(emptyList()) }

    @Provides
    fun pendingEdits(): PendingEdits = PendingEdits { null }

    @Provides
    fun ownStatuses(): OwnStatuses = OwnStatuses { StatusRefresh.OFFLINE }

    @Provides
    @Singleton
    fun sync(app: Application): SubmissionSyncScheduler = SubmissionSyncScheduler(app)
}
