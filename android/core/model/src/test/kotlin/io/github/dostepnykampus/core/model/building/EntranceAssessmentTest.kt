/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.model.building

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

/**
 * Oznaczenie wejścia przyjęte po dniu wysłania zmiany w strefie kampusu.
 *
 * Wymagania: US10.8, BR-23, BR-24
 */
class EntranceAssessmentTest {
    @Test
    fun laterDayIsChange() {
        val sent = Instant.parse("2026-11-20T10:00:00Z")
        assertTrue(assessed(day = 21).isAfterDayOf(sent), "przyjęte dzień po wysłaniu")
        assertFalse(assessed(day = 20).isAfterDayOf(sent), "ten sam dzień")
        assertFalse(assessed(day = 3).isAfterDayOf(sent), "przed wysłaniem")
    }

    @Test
    fun countsCampusDay() {
        val lateEvening = Instant.parse("2026-11-20T23:30:00Z")
        assertFalse(assessed(day = 21).isAfterDayOf(lateEvening), "23:30 UTC to już 21 listopada w Warszawie")
    }

    private fun assessed(day: Int): EntranceAssessment =
        EntranceAssessment(date = LocalDate.of(2026, 11, day), source = AssessmentSource.WALKTHROUGH)
}
