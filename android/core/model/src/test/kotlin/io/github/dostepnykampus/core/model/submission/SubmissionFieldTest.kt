/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.model.submission

import io.github.dostepnykampus.core.model.GeoPoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Pola, którymi różnią się dwie treści jednego zgłoszenia.
 *
 * Wymagania: US9.3, US10.8, BR-3
 */
class SubmissionFieldTest {
    @Test
    fun listsChangedInOrder() {
        val changed = BEFORE.copy(description = "Wysoki próg", severity = 2)
        assertEquals(
            listOf(SubmissionField.SEVERITY, SubmissionField.DESCRIPTION),
            fieldsChanged(previous = BEFORE, current = changed),
            "uciążliwość i opis w kolejności pól",
        )
    }

    @Test
    fun ignoresPhotoOrderAndFalse() {
        val same = BEFORE.copy(photoIds = listOf("b", "a"), isBarrierRemoved = false)
        assertEquals(emptyList<SubmissionField>(), fieldsChanged(previous = BEFORE, current = same), "bez różnic")
    }

    private companion object {
        val BEFORE =
            SubmissionContent(
                kind = SubmissionKind.PRECISE,
                location = GeoPoint(longitude = 17.06, latitude = 51.1),
                photoIds = listOf("a", "b"),
                barrierType = "TB-13",
                severity = 1,
                description = "Próg",
            )
    }
}
