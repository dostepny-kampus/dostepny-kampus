/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submissions

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/** Kolejka zgłoszeń w grafie Hilt, sięgnięta z wątku WorkManagera (NFR-6). */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface SubmissionSyncEntryPoint {
    fun submissionQueue(): SubmissionQueue
}

/**
 * Wysyła kolejkę, gdy telefon wróci do sieci (NFR-6): zadanie czeka na warunek sieci, więc system trzyma je
 * do zasięgu. Gdy wysyłka przerwie się na braku sieci, [CoroutineWorker.Result.retry] prosi o kolejną próbę.
 */
class SubmissionSyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val entryPoint =
            EntryPointAccessors.fromApplication(applicationContext, SubmissionSyncEntryPoint::class.java)
        return entryPoint.submissionQueue().syncResult()
    }
}

/**
 * Wynik zadania po jednej wysyłce kolejki: `retry`, gdy [SubmissionQueue.sendAll] każe spróbować później,
 * a `success`, gdy doszła do końca kolejki (NFR-6). Które odpowiedzi serwera oznaczają „później”, ustala
 * kolejka, nie zadanie (BR-9, BR-26).
 */
internal suspend fun SubmissionQueue.syncResult(): ListenableWorker.Result =
    if (sendAll()) ListenableWorker.Result.success() else ListenableWorker.Result.retry()
