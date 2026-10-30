/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.ui.graphics.Color
import io.github.dostepnykampus.core.model.GeoPoint
import okhttp3.Request
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Pomocniki widoku MapLibre wykonywane w JVM: kamera, nagłówek HTTP i kolory tokenów, na danych testowych. */
class MapLibreConversionTest {
    @Test
    fun nfr53_camera_survives_maplibre() {
        val camera = MapCamera(center = GeoPoint(longitude = 17.0, latitude = 60.0), zoom = START_ZOOM)
        assertEquals(camera, camera.toPosition().toCamera())
    }

    @Test
    fun nfr39_own_user_agent() {
        val tile = Request.Builder().url("https://tile.openstreetmap.org/0/0/0.png").build()
        val call = osmClient(userAgent = "DostepnyKampus/1.0 (io.example.test)").newCall(tile)
        assertEquals("DostepnyKampus/1.0 (io.example.test)", call.request().header("User-Agent"))
    }

    @Test
    fun nfr16_token_color_as_hex() {
        assertEquals("#004E5E", Color(color = 0xFF004E5E).hex())
    }
}
