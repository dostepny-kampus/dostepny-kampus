/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submissions

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * Planuje wysyłkę kolejki po powrocie sieci.
 *
 * Zadanie jest jednorazowe i czeka na internet, więc system uruchomi je w swoim oknie pracy także po
 * zamknięciu aplikacji. Zwołuje je zapis zgłoszenia; start aplikacji powinien zwołać je osobno (`app`), żeby
 * zgłoszenia z poprzedniego procesu wysłały się bez czekania na nowe zgłoszenie. WorkManager pobiera dopiero
 * pierwsze planowanie, więc kolejkę można utworzyć, zanim WorkManager wystartuje.
 *
 * Wymagania: NFR-6
 */
public class SubmissionSyncScheduler(context: Context) {
    private val workManager: WorkManager by lazy { WorkManager.getInstance(context.applicationContext) }

    /** Planuje wysyłkę przy starcie aplikacji; powtórne wywołanie nie dubluje zadania. */
    public fun schedule() {
        enqueue(ExistingWorkPolicy.KEEP)
    }

    /**
     * Planuje wysyłkę po zapisie zgłoszenia. Zadanie, które już działa, mogło przeczytać kolejkę przed zapisem, więc
     * nowe zadanie dochodzi po nim zamiast przepaść; dwa zadania nigdy nie działają naraz.
     */
    public fun scheduleAfterSave() {
        enqueue(ExistingWorkPolicy.APPEND_OR_REPLACE)
    }

    private fun enqueue(policy: ExistingWorkPolicy) {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val work = OneTimeWorkRequestBuilder<SubmissionSyncWorker>().setConstraints(constraints).build()
        workManager.enqueueUniqueWork(SUBMISSION_SYNC_WORK, policy, work)
    }
}

private const val SUBMISSION_SYNC_WORK = "wysylka-zgloszen"
