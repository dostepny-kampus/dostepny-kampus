/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

import androidx.annotation.StringRes
import io.github.dostepnykampus.core.model.submission.SubmissionField

/** Nazwa pola zgłoszenia, np. „Uciążliwość”; wykaz różnic pisze ją małą literą. Wymagania: US9.3, US10.8 */
@StringRes
public fun SubmissionField.nameRes(): Int =
    when (this) {
        SubmissionField.LOCATION -> R.string.pole_miejsce
        SubmissionField.PHOTOS -> R.string.pole_zdjecia
        SubmissionField.BUILDING -> R.string.pole_budynek
        SubmissionField.FLOOR -> R.string.pietro_kolumna
        SubmissionField.BARRIER_GROUP -> R.string.pole_grupa
        SubmissionField.BARRIER_TYPE -> R.string.wybor_typ
        SubmissionField.SEVERITY -> R.string.wybor_uciazliwosc
        SubmissionField.DESCRIPTION -> R.string.pole_opis
        SubmissionField.BARRIER_REMOVED -> R.string.pole_usunieta
        SubmissionField.ENTRANCE -> R.string.pole_wejscie
        SubmissionField.ENTRANCE_MARKING -> R.string.pole_oznaczenie
    }
