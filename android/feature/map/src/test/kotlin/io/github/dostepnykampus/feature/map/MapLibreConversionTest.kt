/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.ui.graphics.Color
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.camera.MapInset
import io.github.dostepnykampus.feature.map.camera.START_ZOOM
import io.github.dostepnykampus.feature.map.camera.hex
import io.github.dostepnykampus.feature.map.camera.toCamera
import io.github.dostepnykampus.feature.map.camera.toPosition
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Pomocniki widoku MapLibre wykonywane w JVM: kamera i kolory tokenów, na danych testowych.
 *
 * Wymagania: NFR-16, NFR-53
 */
class MapLibreConversionTest {
    @Test
    fun keepsCameraThroughMapLibre() {
        val camera = MapCamera(center = GeoPoint(longitude = 17.0, latitude = 60.0), zoom = START_ZOOM)
        assertEquals(camera, camera.toPosition().toCamera())
        val aboveSheet = camera.toPosition(MapInset(top = 180.0, bottom = 600.0))
        assertEquals(camera, aboveSheet.toCamera())
        assertEquals(listOf(0.0, 180.0, 0.0, 600.0), aboveSheet.padding?.toList())
    }

    @Test
    fun writesTokenColorAsHex() {
        assertEquals("#004E5E", Color(color = 0xFF004E5E).hex())
    }
}
