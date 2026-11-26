/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/** Dzień zapisuje się bez zer wiodących, a chwila liczy się w strefie kampusu. Wymagania: US1.5, BR-19 */
class CampusDayTest {
    @Test
    fun writesDayWithoutLeadingZeros() {
        assertEquals("Dzień bez zer wiodących", "7.3.2026", campusDay(LocalDate.of(2026, 3, 7)))
    }

    @Test
    fun countsMomentInCampusZoneNotUtc() {
        assertEquals("Zimą kampus ma UTC+1", "7.3.2026", campusDay(Instant.parse("2026-03-06T23:30:00Z")))
    }

    @Test
    fun countsSummerMomentWithOffset() {
        assertEquals("Latem kampus ma UTC+2", "1.8.2026", campusDay(Instant.parse("2026-07-31T22:30:00Z")))
    }

    /** Dzień i godzina w strefie kampusu, np. ostatnia synchronizacja na C4. Wymagania: NFR-10 */
    @Test
    fun writesMomentWithHour() {
        assertEquals("Dzień i godzina", "12.11.2026, 10:42", campusMoment(Instant.parse("2026-11-12T09:42:00Z")))
    }
}
