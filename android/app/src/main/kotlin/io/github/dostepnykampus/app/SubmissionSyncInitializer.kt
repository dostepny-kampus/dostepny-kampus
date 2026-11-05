/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import android.content.Context
import androidx.startup.Initializer
import androidx.work.WorkManagerInitializer
import io.github.dostepnykampus.core.data.submissions.SubmissionSyncScheduler

/**
 * Planuje wysyłkę kolejki zgłoszeń przy starcie procesu (NFR-6), więc zgłoszenia zapisane przed zamknięciem aplikacji
 * wysyłają się po powrocie sieci bez czekania na nowe zgłoszenie. Uruchamia go `androidx.startup` z manifestu, po
 * inicjalizacji WorkManagera; Robolectric nie tworzy dostawców z manifestu, więc testy ekranów go nie wołają.
 */
class SubmissionSyncInitializer : Initializer<SubmissionSyncScheduler> {
    override fun create(context: Context): SubmissionSyncScheduler =
        SubmissionSyncScheduler(context).also(SubmissionSyncScheduler::schedule)

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf(WorkManagerInitializer::class.java)
}
