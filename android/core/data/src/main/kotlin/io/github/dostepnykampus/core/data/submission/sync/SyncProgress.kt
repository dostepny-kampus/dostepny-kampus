/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submission.sync

import androidx.work.ExistingWorkPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan

/**
 * Postęp trwającego przebiegu wysyłki.
 *
 * @property sent ile wysłano w tym przebiegu
 * @property total ile czekało na początku przebiegu, razem z zapisanymi w jego trakcie
 *
 * Wymagania: US9.1
 */
public data class SyncProgress(val sent: Int, val total: Int)

/** Wysyłka na żądanie i jej postęp. Wymagania: US9.1, BR-9 */
public interface SyncNow {
    /** Postęp trwającego przebiegu; `null`, gdy wysyłka nie trwa. */
    public fun progress(): Flow<SyncProgress?>

    /** Rusza wysyłkę od razu; gdy przebieg już trwa, nic nie robi, więc przebiegi się nie dublują. */
    public suspend fun syncNow()
}

/** [SyncNow] na zadaniu [scheduler] i stanie kolejki [status]. */
internal class ScheduledSync(private val scheduler: SubmissionSyncScheduler, private val status: SyncStatusSource) :
    SyncNow {
    override fun progress(): Flow<SyncProgress?> =
        progressOf(isRunning = scheduler.isRunning(), waiting = status.status().map { it.waiting })

    override suspend fun syncNow() {
        syncNowPolicy(scheduler.isRunning().first())?.let(scheduler::enqueue)
    }
}

/** Postęp z tego, czy przebieg trwa ([isRunning]), i z liczby czekających ([waiting]). */
internal fun progressOf(isRunning: Flow<Boolean>, waiting: Flow<Int>): Flow<SyncProgress?> =
    combine(isRunning, waiting) { running, left -> running to left }
        .scan<Pair<Boolean, Int>, SyncProgress?>(null) { last, (running, left) -> nextProgress(last, running, left) }
        .distinctUntilChanged()

/** Postęp po zmianie: całość nie maleje, a wysłane to całość bez czekających. */
internal fun nextProgress(last: SyncProgress?, isRunning: Boolean, waiting: Int): SyncProgress? {
    if (!isRunning) return null
    val total = maxOf(a = last?.total ?: waiting, b = waiting + (last?.sent ?: 0))
    return SyncProgress(sent = total - waiting, total = total)
}

/** Zastąpienie czekającego zadania nowym, które rusza od razu; `null`, gdy przebieg trwa. */
internal fun syncNowPolicy(isRunning: Boolean): ExistingWorkPolicy? =
    if (isRunning) null else ExistingWorkPolicy.REPLACE
