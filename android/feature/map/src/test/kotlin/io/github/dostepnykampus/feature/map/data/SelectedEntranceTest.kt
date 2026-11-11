/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.data

import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.BuildingCatalog
import io.github.dostepnykampus.core.model.Entrance
import io.github.dostepnykampus.core.model.EntranceMarking
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Geometry

private const val UNNAMED = "Wejście bez nazwy w danych"

/** Dane wejść dla mapy V1 z syntetycznego katalogu „Budynek testowy” (nie dane PWr): US2.1, US3.3, decyzje 134, 135. */
class SelectedEntranceTest {
    private val at = GeoPoint(longitude = 0.25, latitude = 0.5)
    private val ring =
        listOf(
            GeoPoint(longitude = 0.0, latitude = 0.0),
            GeoPoint(longitude = 1.0, latitude = 0.0),
            GeoPoint(longitude = 1.0, latitude = 1.0),
            GeoPoint(longitude = 0.0, latitude = 0.0),
        )
    private val buildings =
        listOf("A-1", "A-2").map { Building(symbol = it, name = "Budynek testowy", outline = GeoPolygon(listOf(ring))) }
    private val catalog =
        BuildingCatalog(
            buildings = buildings,
            entrances =
                listOf(
                    entrance(id = "node/1", building = "A-1", name = "Wejście dostosowane"),
                    entrance(id = "node/1", building = "A-2", name = "Wejście dostosowane"),
                    entrance(id = "node/2", building = "A-1", name = null),
                ),
            attribution = "",
        )

    private fun chosen(id: String, building: String = "A-1") =
        selectedEntrance(catalog = catalog, id = id, buildingSymbol = building, unnamed = UNNAMED)

    /** Wiersz E1: każdy znak na mapie ma identyfikator wejścia, więc da się go wskazać. */
    @Test
    fun us2_1_feature_has_id() {
        val json = campusMapData(catalog).entrances.toString()
        val ids = FeatureCollection.fromJson<Geometry?, JsonObject?>(json).features.map { it.properties?.get("id") }
        assertEquals(listOf("node/1", "node/2"), ids.map { it?.run { jsonPrimitive.content } })
    }

    /** Wiersz E2: pinezka stoi w punkcie wejścia, a podpis to nazwa wejścia. */
    @Test
    fun us3_3_selected_has_point_name() {
        val expected =
            SelectedEntrance(id = "node/1", location = at, label = "Wejście dostosowane", buildingSymbol = "A-1")
        assertEquals(expected, chosen("node/1"))
    }

    /** Wiersz E4: wejście wspólne kilku budynków podaje budynek ekranu, z którego użyto przycisku (decyzja 135). */
    @Test
    fun us3_3_shared_screen_building() {
        assertEquals("A-2", chosen("node/1", building = "A-2")?.buildingSymbol)
    }

    /** Wiersz E2: wejście bez nazwy w danych dostaje podpis zastępczy. */
    @Test
    fun us3_3_unnamed_label() {
        assertEquals(UNNAMED, chosen("node/2")?.label)
    }

    /** Brzeg E2: identyfikator spoza katalogu niczego nie zaznacza. */
    @Test
    fun us3_3_unknown_id_nothing() {
        assertNull(chosen("node/99"))
    }

    private fun entrance(id: String, building: String, name: String?) =
        Entrance(
            id = id,
            buildingSymbol = building,
            name = name,
            isMain = false,
            type = "yes",
            location = at,
            marking = EntranceMarking.ACCESSIBLE,
        )
}
