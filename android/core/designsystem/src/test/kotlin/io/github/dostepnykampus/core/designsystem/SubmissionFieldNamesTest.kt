/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.dostepnykampus.core.model.submission.SubmissionField
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Nazwy pól zgłoszenia w wykazie różnic, małą literą. Wymagania: US9.3, US10.8 */
@RunWith(RobolectricTestRunner::class)
class SubmissionFieldNamesTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun namesEveryField() {
        assertEquals(
            "nazwy pól",
            listOf(
                "miejsce",
                "zdjęcia",
                "budynek",
                "piętro",
                "grupa barier",
                "typ bariery",
                "uciążliwość",
                "opis",
                "bariera usunięta",
                "wejście",
                "oznaczenie wejścia",
            ),
            SubmissionField.entries.map { context.getString(it.nameRes()) },
        )
    }
}
