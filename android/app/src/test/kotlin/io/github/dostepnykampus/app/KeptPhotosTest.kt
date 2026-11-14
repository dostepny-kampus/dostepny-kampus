/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import io.github.dostepnykampus.feature.capture.navigation.ReportRoute
import io.github.dostepnykampus.feature.capture.navigation.ReportStep
import io.github.dostepnykampus.feature.capture.report.ReportDraft
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Odtworzona historia podaje sprzątaniu pliki zdjęć zgłoszenia szybkiego, których nie wolno usunąć.
 *
 * Wymagania: NFR-5
 */
class KeptPhotosTest {
    @Test
    fun keepsQuickReportPhoto() {
        val routes = listOf(MapRoute(), ReportRoute(step = ReportStep.GROUP, photoId = "z-1", photoPath = "/z/z-1.jpg"))

        assertEquals(setOf("/z/z-1.jpg"), keptPhotos(routes = routes, draft = ReportDraft()))
    }

    @Test
    fun keepsNothingWithoutDraft() {
        assertEquals(emptySet<String>(), keptPhotos(routes = listOf(MapRoute(), ReportRoute()), draft = ReportDraft()))
    }
}
