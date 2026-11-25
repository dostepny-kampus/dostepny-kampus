/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.dostepnykampus.core.model.submission.SubmissionStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Każdy status zgłoszenia z serwera ma własne słowo i własną ikonę. Wymagania: NFR-21 */
@RunWith(RobolectricTestRunner::class)
class SubmissionStatusNamesTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun statusesHaveWords() {
        assertEquals(
            "słowa statusów",
            listOf("Wysłane", "W weryfikacji", "Przyjęte", "Odrzucone", "Archiwalne"),
            SubmissionStatus.entries.map { context.getString(it.nameRes()) },
        )
    }

    @Test
    fun statusesHaveOwnIcons() {
        val icons = SubmissionStatus.entries.map { it.icon() }
        assertEquals("osobna ikona każdego statusu", SubmissionStatus.entries.size, icons.toSet().size)
        assertEquals("ikona „Przyjęte”", StatusIcons.Accepted, SubmissionStatus.ACCEPTED.icon())
    }
}
