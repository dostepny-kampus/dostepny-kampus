/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submission.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.github.dostepnykampus.core.data.account.AccountSession
import io.github.dostepnykampus.core.data.inspection.MarkerSync
import io.github.dostepnykampus.core.data.inspection.syncResult
import java.time.Instant

/** Kolejki zgłoszeń i znaczników w grafie Hilt, sięgnięte z wątku WorkManagera. Wymagania: NFR-6 */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface SubmissionSyncEntryPoint {
    fun submissionQueue(): SubmissionQueue

    fun markerSync(): MarkerSync

    fun accountSession(): AccountSession

    fun syncLog(): SyncLog
}

/**
 * Wysyła kolejkę zgłoszeń i kolejkę znaczników piętra obejrzanego, gdy telefon wróci do sieci: zadanie czeka
 * na warunek sieci, więc system trzyma je do zasięgu. Gdy któraś wysyłka przerwie się na braku sieci albo awarii
 * serwera, [CoroutineWorker.Result.retry] prosi o kolejną próbę. Wynik przebiegu trafia do [SyncLog]; przebieg
 * czekający na zalogowanie nie doszedł do końca kolejki.
 *
 * Wymagania: NFR-6, US15.3, US9.1
 */
internal class SubmissionSyncWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val entryPoint =
            EntryPointAccessors.fromApplication(applicationContext, SubmissionSyncEntryPoint::class.java)
        entryPoint.accountSession().awaitRestore()
        val submissions = entryPoint.submissionQueue().syncResult()
        val markers = entryPoint.markerSync().syncResult()
        val isRetry = Result.retry() in listOf(submissions, markers)
        val isDone = !isRetry && !entryPoint.submissionQueue().loginExpired.value
        entryPoint.syncLog().record(isDone = isDone, at = Instant.now())
        return if (isRetry) Result.retry() else Result.success()
    }
}

/**
 * Wynik zadania po jednej wysyłce kolejki: `retry`, gdy [SubmissionQueue.sendAll] każe spróbować później,
 * a `success`, gdy doszła do końca kolejki. Które odpowiedzi serwera oznaczają „później”, ustala
 * kolejka, nie zadanie.
 *
 * Wymagania: NFR-6, BR-9, BR-26
 */
internal suspend fun SubmissionQueue.syncResult(): ListenableWorker.Result =
    if (sendAll()) ListenableWorker.Result.success() else ListenableWorker.Result.retry()
