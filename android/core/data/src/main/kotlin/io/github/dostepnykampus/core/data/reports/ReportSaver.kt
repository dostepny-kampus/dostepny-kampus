/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.reports

import android.database.sqlite.SQLiteException
import io.github.dostepnykampus.core.data.submissions.EditedSubmission
import io.github.dostepnykampus.core.data.submissions.PendingPhotoDraft
import io.github.dostepnykampus.core.model.SubmissionContent
import java.io.IOException

/**
 * Zapis treści i zdjęć zgłoszenia w kolejce telefonu, bez udostępniania typów bazy.
 *
 * Wymagania: NFR-5, US9.1
 */
fun interface ReportSaver {
    /**
     * Zapisuje treść i zdjęcia; rzuca [IOException] albo [SQLiteException], gdy telefon nie może zapisać.
     *
     * Wymagania: NFR-7, US8.3
     *
     * @param content treść zgłoszenia
     * @param photos zdjęcia zgłoszenia
     * @param edited poprawiane zgłoszenie z C3 albo `null` dla nowego
     */
    suspend fun save(
        content: SubmissionContent,
        photos: List<PendingPhotoDraft>,
        edited: EditedSubmission?,
    )
}
