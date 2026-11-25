/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.dostepnykampus.core.data.submission.conflict.StoredVersionConflicts
import io.github.dostepnykampus.core.data.submission.conflict.VersionConflicts
import io.github.dostepnykampus.core.data.submission.sync.SubmissionStore
import io.github.dostepnykampus.core.data.submission.sync.SubmissionSyncScheduler
import io.github.dostepnykampus.core.network.auth.AccessTokens
import kotlin.coroutines.CoroutineContext

/** Konflikty wersji zgłoszeń `core:data` w grafie Hilt aplikacji. */
@Module
@InstallIn(SingletonComponent::class)
internal object ConflictDataModule {
    /** Konflikty wersji zgłoszeń zalogowanego konta, z wysyłką po wyborze własnej wersji. Wymagania: US9.3, BR-10 */
    @Provides
    fun versionConflicts(
        store: SubmissionStore,
        sync: SubmissionSyncScheduler,
        tokens: AccessTokens,
        @IoContext ioContext: CoroutineContext,
    ): VersionConflicts =
        StoredVersionConflicts(
            store = store,
            ioContext = ioContext,
            subject = tokens::subject,
            onSaved = sync::scheduleAfterSave,
        )
}
