/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.ui.graphics.Color
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.camera.START_ZOOM
import io.github.dostepnykampus.feature.map.camera.hex
import io.github.dostepnykampus.feature.map.camera.toCamera
import io.github.dostepnykampus.feature.map.camera.toPosition
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Pomocniki widoku MapLibre wykonywane w JVM: kamera i kolory tokenów, na danych testowych. */
class MapLibreConversionTest {
    @Test
    fun nfr53_camera_survives_maplibre() {
        val camera = MapCamera(center = GeoPoint(longitude = 17.0, latitude = 60.0), zoom = START_ZOOM)
        assertEquals(camera, camera.toPosition().toCamera())
        val aboveSheet = camera.copy(bottomInset = 600.0)
        assertEquals(aboveSheet, aboveSheet.toPosition().toCamera())
    }

    @Test
    fun nfr16_token_color_as_hex() {
        assertEquals("#004E5E", Color(color = 0xFF004E5E).hex())
    }
}
