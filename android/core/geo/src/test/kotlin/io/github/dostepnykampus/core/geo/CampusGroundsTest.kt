/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.geo

import io.github.dostepnykampus.core.geo.building.parseBuildingCatalog
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.math.cos

/** Teren, na którym wolno złożyć zgłoszenie: wnętrze granicy i jej linia, jak `ST_Covers`. Wymagania: BR-G5, US4.9 */
class CampusGroundsTest {
    /** Punkt wewnątrz i punkt na linii granicy należą do terenu, punkt tuż za linią nie. */
    @Test
    fun coversInsideAndEdge() {
        val corner = GeoPoint(longitude = 17.0, latitude = 51.0)
        val far = GeoPoint(longitude = 17.01, latitude = 51.01)
        val square = listOf(corner, corner.copy(longitude = 17.01), far, corner.copy(latitude = 51.01), corner)
        val grounds = campusGrounds(mapOf("Teren" to GeoPolygon(listOf(square))))
        assertTrue(grounds.covers(GeoPoint(longitude = 17.005, latitude = 51.005)), "wnętrze granicy")
        assertTrue(grounds.covers(GeoPoint(longitude = 17.005, latitude = 51.0)), "punkt na linii granicy")
        assertFalse(grounds.covers(GeoPoint(longitude = 17.005, latitude = 50.9999)), "punkt tuż za linią")
    }

    /**
     * Na danych PWr: każdy punkt konturu budynku leży na terenie, a punkt 20 m za najdalej na wschód wysuniętym
     * punktem granicy „Kampusu głównego” nie.
     * Scenariusz (częściowo): zgloszenie-szybkie.feature: Zgłoszenie szybkie tylko na terenie PWr
     */
    @Test
    fun refusesPointBeyondPwrEdge() {
        val catalog = parseBuildingCatalog(File(System.getProperty("dostepnykampus.buildings")).readText())
        val outlines = catalog.buildings.flatMap { it.outline.allRings().flatten() }
        assertTrue(outlines.all(PwrGrounds::covers), "kontury budynków na terenie PWr")
        val main = PwrAreaBoundaries.getValue("Kampus główny").allRings()
        val east = main.flatten().maxBy { it.longitude }
        assertTrue(PwrGrounds.covers(east), "punkt granicy")
        val beyond = east.copy(longitude = east.longitude + METERS_20 / cos(Math.toRadians(east.latitude)))
        assertFalse(PwrGrounds.covers(beyond), "20 m za granicą")
    }

    private companion object {
        const val METERS_20 = 20.0 / 111_320.0
    }
}
