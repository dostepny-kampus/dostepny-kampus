/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.di

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.dostepnykampus.core.data.inspection.FloorMarkers
import io.github.dostepnykampus.core.data.submission.sync.ScheduledSync
import io.github.dostepnykampus.core.data.submission.sync.SubmissionStore
import io.github.dostepnykampus.core.data.submission.sync.SubmissionSyncScheduler
import io.github.dostepnykampus.core.data.submission.sync.SyncLog
import io.github.dostepnykampus.core.data.submission.sync.SyncNow
import io.github.dostepnykampus.core.data.submission.sync.SyncStatusSource
import io.github.dostepnykampus.core.data.submission.sync.syncDataStore
import io.github.dostepnykampus.core.data.submission.sync.syncStatusOf
import io.github.dostepnykampus.core.network.auth.AccessTokens
import kotlinx.coroutines.flow.map
import javax.inject.Singleton

/** Stan kolejki wysyłki `core:data` w grafie Hilt aplikacji. */
@Module
@InstallIn(SingletonComponent::class)
internal object SyncDataModule {
    /** Jedyny dziennik przebiegów wysyłki w grafie. Wymagania: US9.1, NFR-10 */
    @Provides
    @Singleton
    fun syncLog(app: Application): SyncLog = SyncLog(syncDataStore(app))

    /** Stan kolejki zgłoszeń i znaczników zalogowanego konta. Wymagania: US9.1, BR-9 */
    @Provides
    fun syncStatus(
        store: SubmissionStore,
        markers: FloorMarkers,
        tokens: AccessTokens,
        log: SyncLog,
    ): SyncStatusSource =
        syncStatusOf(
            pending = store.pending.observe().map { rows -> rows.map { it.submission } },
            markers = markers.pending(),
            subjects = tokens.subjects,
            log = log,
        )

    /** „Synchronizuj teraz” i postęp przebiegu na zadaniu wysyłki. Wymagania: US9.1, BR-9 */
    @Provides
    fun syncNow(scheduler: SubmissionSyncScheduler, status: SyncStatusSource): SyncNow =
        ScheduledSync(scheduler = scheduler, status = status)
}
