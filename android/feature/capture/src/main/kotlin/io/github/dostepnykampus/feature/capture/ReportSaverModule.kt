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
import io.github.dostepnykampus.feature.capture.quick.content

/** Formularz C2 zapisuje do kolejki zgłoszeń z `core:data`. Wymagania: NFR-5 */
@Module
@InstallIn(SingletonComponent::class)
object ReportSaverModule {
    /**
     * Zapis przez kolejkę; poprawka z C3 zastępuje swoje zgłoszenie. Wynik nie jest ekranowi potrzebny.
     *
     * Wymagania: US8.3
     */
    @Provides
    fun reportSaver(queue: SubmissionQueue): ReportSaver =
        ReportSaver { content, photos, edited ->
            if (edited == null) queue.save(content, photos) else queue.saveEdit(edited, content, photos)
        }
}
