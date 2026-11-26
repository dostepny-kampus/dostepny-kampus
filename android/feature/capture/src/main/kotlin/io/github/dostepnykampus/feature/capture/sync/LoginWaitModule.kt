/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.sync

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.dostepnykampus.core.data.submission.sync.SubmissionQueue

/** C4 czyta wygasłe zalogowanie z kolejki zgłoszeń `core:data`. Wymagania: US9.4, BR-26 */
@Module
@InstallIn(SingletonComponent::class)
object LoginWaitModule {
    /** Odmowa 401 zatrzymuje kolejkę do ponownego zalogowania. */
    @Provides
    fun loginWait(queue: SubmissionQueue): LoginWait = LoginWait { queue.loginExpired }
}
