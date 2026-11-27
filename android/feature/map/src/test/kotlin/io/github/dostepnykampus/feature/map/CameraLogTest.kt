/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import io.github.dostepnykampus.core.model.CampusBounds
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.map.camera.CameraMotion
import io.github.dostepnykampus.feature.map.camera.CameraTransition
import io.github.dostepnykampus.feature.map.camera.FreeMap
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.camera.MapInset
import io.github.dostepnykampus.feature.map.camera.MapLimits
import io.github.dostepnykampus.feature.map.camera.endLine
import io.github.dostepnykampus.feature.map.camera.pickLine
import io.github.dostepnykampus.feature.map.camera.startLine
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Linie logu ruchu kamery V1: cel z terenem i granicą, start z dopełnieniem, koniec i przerwanie.
 *
 * Wymagania: US2.8, NFR-56
 */
class CameraLogTest {
    private val camera = MapCamera(center = GeoPoint(longitude = 17.06, latitude = 51.109), zoom = 18.2)
    private val move = CameraTransition(camera = camera, motion = CameraMotion.FLIGHT, durationMillis = 1300L)
    private val free = FreeMap(widthDp = 360.0, heightDp = 412.4, endDp = 0.0, marginDp = 16.0)

    /** Wiersz: V1, cel budynku z innego terenu pokazuje teren i granicę, z którymi go przycięto. */
    @Test
    fun describesPickWithAreaAndBorder() {
        val border = CampusBounds(west = 17.05, south = 51.1, east = 17.07, north = 51.12)
        val limits = MapLimits(bounds = border, widthDp = 360.0, heightDp = 640.0)
        val line = pickLine(building = "B-2", area = "Wittigowo", limits = limits, free = free, move = move)
        val expected =
            "Kamera: cel B-2, teren Wittigowo, granica 17.05000,51.10000..17.07000,51.12000, " +
                "wolna mapa 360×412 dp, FLIGHT 1300 ms do 51.10900,17.06000 z18.20"
        assertEquals(expected, line, "Linia celu podaje teren, granicę, wolną mapę i ruch")
    }

    /** Wiersz: V1, cel na terenie startowym bez granicy nazywa oba braki słowami. */
    @Test
    fun namesStartAreaWithoutBorder() {
        val line = pickLine(building = "A-1", area = null, limits = null, free = free, move = move)
        val expected =
            "Kamera: cel A-1, teren startowy, granica brak, wolna mapa 360×412 dp, " +
                "FLIGHT 1300 ms do 51.10900,17.06000 z18.20"
        assertEquals(expected, line, "Teren startowy i brak granicy są nazwane słowami")
    }

    /** Wiersz: V1, start ruchu podaje dopełnienie karty w pikselach. */
    @Test
    fun describesStartWithPadding() {
        val line = startLine(move, MapInset(top = 0.0, bottom = 905.6))
        val expected = "Kamera: start FLIGHT 1300 ms do 51.10900,17.06000 z18.20, dopełnienie góra 0 px, dół 906 px"
        assertEquals(expected, line, "Start ruchu podaje rodzaj, czas, cel i dopełnienie")
    }

    /** Wiersz: V1, przerwanie ruchu zastąpionego nowym różni się od końca bieżącego ruchu. */
    @Test
    fun marksReplacedMoveAsInterrupted() {
        val line = endLine(isDone = false, isCurrent = false, at = camera)
        val expected = "Kamera: przerwany (zastąpiony) w 51.10900,17.06000 z18.20"
        assertEquals(expected, line, "Ruch zastąpiony jest oznaczony")
    }
}
