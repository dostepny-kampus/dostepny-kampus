/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.dostepnykampus.core.data.submissions.SubmissionQueue

/** Formularz C2 zapisuje do kolejki zgłoszeń z `core:data` (NFR-5). */
@Module
@InstallIn(SingletonComponent::class)
object ReportSaverModule {
    /** Zapis przez kolejkę; wynik zapisu nie jest ekranowi potrzebny. */
    @Provides
    fun reportSaver(queue: SubmissionQueue): ReportSaver =
        ReportSaver { content, photos ->
            queue.save(content, photos)
        }
}
