/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submissions

import io.github.dostepnykampus.core.database.OwnSubmissionDao
import io.github.dostepnykampus.core.database.PendingEditDao
import io.github.dostepnykampus.core.database.PendingSubmissionDao
import javax.inject.Inject

/**
 * Zgłoszenia zapisane w telefonie: kolejka wysyłki, historia Zbierającego i poprawki z kolejki. Daje go Hilt;
 * [SubmissionQueue] i źródła listy C3 budują się z niego, więc tabele bazy nie wychodzą poza `core:data`.
 *
 * Wymagania: NFR-5, US8, US8.3
 *
 * @property pending wiersze kolejki wysyłki
 * @property history historia zgłoszeń Zbierającego dla listy C3
 * @property edits zgłoszenia z kolejki czytane do poprawki w C2
 */
@ConsistentCopyVisibility
data class SubmissionStore
    @Inject
    internal constructor(
        internal val pending: PendingSubmissionDao,
        internal val history: OwnSubmissionDao,
        internal val edits: PendingEditDao,
    )
