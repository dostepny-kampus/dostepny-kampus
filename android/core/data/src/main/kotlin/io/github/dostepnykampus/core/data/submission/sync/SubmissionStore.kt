/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submission.sync

import io.github.dostepnykampus.core.database.cache.ServerCopyDao
import io.github.dostepnykampus.core.database.submission.OwnSubmissionDao
import io.github.dostepnykampus.core.database.submission.PendingEditDao
import io.github.dostepnykampus.core.database.submission.PendingSubmissionDao
import javax.inject.Inject

/**
 * Zgłoszenia zapisane w telefonie: kolejka wysyłki, historia Zbierającego, poprawki z kolejki i wersje z serwera
 * przy konflikcie. Daje go Hilt;
 * [SubmissionQueue] i źródła listy C3 budują się z niego, więc tabele bazy nie wychodzą poza `core:data`.
 *
 * Wymagania: NFR-5, US8, US8.3, US9.3
 *
 * @property pending wiersze kolejki wysyłki
 * @property history historia zgłoszeń Zbierającego dla listy C3
 * @property edits zgłoszenia z kolejki czytane do poprawki w C2
 * @property conflicts wersje z serwera zapisane po odmowie 409
 */
@ConsistentCopyVisibility
public data class SubmissionStore
    @Inject
    internal constructor(
        internal val pending: PendingSubmissionDao,
        internal val history: OwnSubmissionDao,
        internal val edits: PendingEditDao,
        internal val conflicts: ServerCopyDao,
    )
