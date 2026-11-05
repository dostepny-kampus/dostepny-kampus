/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
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
 * Planuje wysyłkę kolejki po powrocie sieci (NFR-6).
 *
 * Zadanie jest jednorazowe i czeka na internet, więc system uruchomi je w swoim oknie pracy także po
 * zamknięciu aplikacji. Zwołuje je zapis zgłoszenia; start aplikacji powinien zwołać je osobno (`app`), żeby
 * zgłoszenia z poprzedniego procesu wysłały się bez czekania na nowe zgłoszenie.
 */
class SubmissionSyncScheduler(context: Context) {
    private val workManager: WorkManager = WorkManager.getInstance(context)

    /** Planuje wysyłkę; powtórne wywołanie nie dubluje zadania. */
    fun schedule() {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val work = OneTimeWorkRequestBuilder<SubmissionSyncWorker>().setConstraints(constraints).build()
        workManager.enqueueUniqueWork(SUBMISSION_SYNC_WORK, ExistingWorkPolicy.KEEP, work)
    }
}

private const val SUBMISSION_SYNC_WORK = "wysylka-zgloszen"
