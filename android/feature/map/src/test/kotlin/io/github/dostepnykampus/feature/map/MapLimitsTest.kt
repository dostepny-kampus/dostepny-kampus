/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import io.github.dostepnykampus.feature.map.data.CampusBounds
import io.github.dostepnykampus.feature.map.data.MapLimits
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.log2

/** Najmniejsze powiększenie obszaru kampusu V1 na telefonie o dużej gęstości i na wysokiej szerokości. */
class MapLimitsTest {
    private val span = 360.0 / 16_384

    /**
     * Wiersz: V1, najmniejsze powiększenie pokazuje cały obszar kampusu także na telefonie, na którym dp to kilka
     * pikseli; przy gęstości 3 widok 1200 × 2400 pikseli ma 400 × 800 dp, a 3072 × 2304 ma 1024 × 768 dp.
     *
     * Wymagania: NFR-39
     */
    @Test
    fun nfr39_min_zoom_in_dp() {
        val density = Density(density = 3f)
        val wide = CampusBounds(west = 0.0, south = -span / 8, east = span, north = span / 8)
        val tall = CampusBounds(west = 0.0, south = -span / 2, east = span / 8, north = span / 2)
        val portrait = density.mapLimits(wide, IntSize(width = 1200, height = 2400))
        val landscape = density.mapLimits(tall, IntSize(width = 3072, height = 2304))
        assertEquals(14.0 + log2(400.0 / 512), portrait?.minZoom ?: 0.0, ZOOM_DELTA)
        assertEquals(14.0 + log2(768.0 / 512), landscape?.minZoom ?: 0.0, ZOOM_DELTA)
    }

    /**
     * Wiersz: V1, na 60° szerokości obszar w rzucie mapy jest dwa razy wyższy niż w stopniach, więc najmniejsze
     * powiększenie obszaru ograniczonego wysokością jest o jeden poziom niższe niż na równiku.
     *
     * Wymagania: NFR-39
     */
    @Test
    fun nfr39_min_zoom_at_60_deg() {
        val tall = CampusBounds(west = 17.0, south = 60.0 - span / 4, east = 17.0 + span / 8, north = 60.0 + span / 4)
        assertEquals(14.0 + log2(1.5), MapLimits(tall, widthDp = 1024.0, heightDp = 768.0).minZoom, ZOOM_DELTA)
    }

    private companion object {
        const val ZOOM_DELTA = 1e-3
    }
}
