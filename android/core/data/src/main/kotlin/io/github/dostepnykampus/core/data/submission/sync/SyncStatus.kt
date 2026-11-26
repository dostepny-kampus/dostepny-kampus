/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submission.sync

import io.github.dostepnykampus.core.data.inspection.PendingMarkerChange
import io.github.dostepnykampus.core.database.submission.PendingSubmissionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant

/**
 * Stan kolejki wysyłki zalogowanego konta.
 *
 * @property submissions zgłoszenia czekające w telefonie na wysłanie, bez odrzuconych przez serwer i bez
 *   czekających na wybór wersji
 * @property markers zmiany znaczników pięter czekające na wysłanie
 * @property lastSyncedAt chwila ostatniej wysyłki, która doszła do końca kolejki; `null` przed pierwszą
 * @property isLastRunFailed czy ostatnia wysyłka przerwała się przed końcem kolejki
 *
 * Wymagania: US9.1, BR-9, BR-10, BR-26, NFR-10
 */
public data class SyncStatus(
    val submissions: Int,
    val markers: Int,
    val lastSyncedAt: Instant?,
    val isLastRunFailed: Boolean,
) {
    /** Wszystko, co czeka na wysłanie. */
    val waiting: Int get() = submissions + markers
}

/** Źródło stanu kolejki wysyłki. Wymagania: US9.1 */
public fun interface SyncStatusSource {
    /** Stan zmieniający się po każdym zapisie, wysyłce i zmianie konta. */
    public fun status(): Flow<SyncStatus>
}

/**
 * Stan z kolejki zgłoszeń [pending], kolejki znaczników [markers] i dziennika przebiegów [log]; liczy tylko wiersze
 * konta z [subjects], a bez konta nic nie czeka.
 */
internal fun syncStatusOf(
    pending: Flow<List<PendingSubmissionEntity>>,
    markers: Flow<List<PendingMarkerChange>>,
    subjects: Flow<String?>,
    log: SyncLog,
): SyncStatusSource =
    SyncStatusSource {
        combine(flow = pending, flow2 = markers, flow3 = subjects, flow4 = log.runs()) { rows, changes, owner, run ->
            val submissions = if (owner == null) 0 else rows.count { it.ownerSub == owner && !it.isRefused }
            SyncStatus(
                submissions = submissions,
                markers = if (owner == null) 0 else changes.size,
                lastSyncedAt = run.lastSyncedAt,
                isLastRunFailed = run.isLastRunFailed,
            )
        }
    }
