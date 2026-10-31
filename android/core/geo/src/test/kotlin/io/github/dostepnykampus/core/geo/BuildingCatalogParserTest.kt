/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.geo

import io.github.dostepnykampus.core.model.EntranceMarking
import io.github.dostepnykampus.core.model.GeoMultiPolygon
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.io.File

/**
 * Katalog budynków z małego syntetycznego GeoJSON w formacie `data/buildings` („Budynek testowy”, nie dane PWr)
 * i z prawdziwego pliku konturów OSM z `data/`.
 */
class BuildingCatalogParserTest {
    private val catalog =
        parseBuildingCatalog(
            collection(
                building(id = 1, symbols = C13, name = "\"Budynek testowy\"", geometry = square(x = 0)),
                building(id = 2, symbols = """["H-14"]""", name = "null", geometry = square(x = 2)),
                building(id = 3, symbols = """["H-14"]""", name = "\"H-14\"", geometry = square(x = 4)),
                building(id = 4, symbols = """["C-3","C-4"]""", name = "\"C-3/C-4\"", geometry = square(x = 6)),
                entrance(id = 5, symbols = C13, tags = """"entrance":"main","wheelchair":"yes""""),
                entrance(id = 6, symbols = C13, tags = """"entrance":"yes","ref":"G","wheelchair":"no""""),
                entrance(id = 7, symbols = C13, tags = """"entrance":"yes","wheelchair":"limited""""),
                entrance(id = 8, symbols = """["C-3","C-4"]""", tags = """"entrance":"main","wheelchair":null"""),
            ),
        )

    private val real by lazy { parseBuildingCatalog(File(System.getProperty("dostepnykampus.buildings")).readText()) }

    @Test
    fun us1_building_per_symbol() {
        assertEquals(listOf("C-13", "H-14", "C-3", "C-4"), catalog.buildings.map { it.symbol })
        assertEquals("Budynek testowy", catalog.buildings.first().name)
        assertEquals("© OpenStreetMap contributors", catalog.attribution)
    }

    @Test
    fun us1_same_symbol_merges() {
        val h14 = catalog.buildings.single { it.symbol == "H-14" }
        assertEquals(GeoMultiPolygon(listOf(polygon(x = 2), polygon(x = 4))), h14.outline)
        assertEquals(null, h14.name)
    }

    @Test
    fun us1_shared_outline() {
        val (c3, c4) = catalog.buildings.filter { it.symbol in setOf("C-3", "C-4") }
        assertEquals(polygon(x = 6), c3.outline)
        assertEquals(c3.outline, c4.outline)
        assertEquals(null, c4.name)
    }

    @Test
    fun us17_1_br_24_marking() {
        val c13 = catalog.entrances.filter { it.buildingSymbol == "C-13" }
        assertEquals(
            listOf(EntranceMarking.ACCESSIBLE, EntranceMarking.INACCESSIBLE, EntranceMarking.NO_DATA),
            c13.map { it.marking },
        )
        assertEquals(listOf(true, false, false), c13.map { it.isMain })
        assertEquals(listOf("main", "yes", "yes"), c13.map { it.type })
        assertEquals(listOf(null, "G", null), c13.map { it.name })
        assertEquals(GeoPoint(longitude = 1.0, latitude = 0.0), c13.first().location)
    }

    @Test
    fun us17_1_shared_entrance() {
        val shared = catalog.entrances.filter { it.id == "node/8" }
        assertEquals(listOf("C-3", "C-4"), shared.map { it.buildingSymbol })
        assertEquals(listOf(EntranceMarking.NO_DATA, EntranceMarking.NO_DATA), shared.map { it.marking })
    }

    @ParameterizedTest
    @CsvSource("yes, ACCESSIBLE", "designated, ACCESSIBLE", "no, INACCESSIBLE", "limited, NO_DATA", "inne, NO_DATA")
    fun br_24_wheelchair_marking(value: String, marking: EntranceMarking) {
        val json = collection(entrance(id = 1, symbols = C13, tags = """"entrance":"yes","wheelchair":"$value""""))
        assertEquals(listOf(marking), parseBuildingCatalog(json).entrances.map { it.marking })
    }

    @ParameterizedTest
    @CsvSource(
        "C-13, ",
        "C-3/C-4, ",
        "T-22, ",
        "C-3 / D-8a, ",
        "A-1 / Budynek główny, A-1 / Budynek główny",
        "M-6 bis, M-6 bis",
    )
    fun us1_symbol_name_is_no_name(name: String, expected: String?) {
        val json = collection(building(id = 1, symbols = """["T-18"]""", name = "\"$name\"", geometry = square(x = 0)))
        assertEquals(expected, parseBuildingCatalog(json).buildings.single().name)
    }

    @Test
    fun us1_bad_format_rejected() {
        listOf(
            """{"type":"FeatureCollection","features":[]}""",
            collection(feature(properties = """{"kind":"droga","symbols":$C13,"osm_id":"way/1"}""", geometry = POINT)),
            collection(building(id = 1, symbols = "[]", name = "null", geometry = square(x = 0))),
            collection(building(id = 1, symbols = "[null]", name = "null", geometry = square(x = 0))),
            collection(building(id = 1, symbols = C13, name = "null", geometry = POINT)),
            collection(feature(properties = """{"kind":"entrance","symbols":$C13,"osm_id":"n/1"}""", geometry = AREA)),
        ).forEach { json -> assertThrows<IllegalArgumentException>(json) { parseBuildingCatalog(json) } }
    }

    @Test
    fun us1_real_file_as_readme() {
        val entrances = real.entrances.distinctBy { it.id }
        assertEquals(95, real.buildings.size)
        assertEquals(72, entrances.size)
        assertEquals(1, entrances.count { it.marking == EntranceMarking.INACCESSIBLE })
    }

    private fun collection(vararg features: String): String =
        """{"type":"FeatureCollection","attribution":"© OpenStreetMap contributors",""" +
            """"features":[${features.joinToString(",")}]}"""

    private fun building(id: Int, symbols: String, name: String, geometry: String): String =
        feature(
            properties = """{"kind":"building","symbols":$symbols,"name":$name,"osm_id":"way/$id"}""",
            geometry = geometry,
        )

    private fun entrance(id: Int, symbols: String, tags: String): String =
        feature(properties = """{"kind":"entrance","symbols":$symbols,"osm_id":"node/$id",$tags}""", geometry = POINT)

    private fun feature(properties: String, geometry: String): String =
        """{"type":"Feature","properties":$properties,"geometry":$geometry}"""

    private fun square(x: Int): String =
        """{"type":"Polygon","coordinates":[[${ring(x).joinToString(",") { (lon, lat) -> "[$lon,$lat]" }}]]}"""

    private fun ring(x: Int): List<Pair<Int, Int>> = listOf(x to 0, x + 1 to 0, x + 1 to 1, x to 1, x to 0)

    private fun polygon(x: Int): GeoPolygon =
        GeoPolygon(listOf(ring(x).map { (lon, lat) -> GeoPoint(longitude = lon * 1.0, latitude = lat * 1.0) }))

    private companion object {
        const val C13 = """["C-13"]"""
        const val AREA = """{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,1],[0,0]]]}"""
        const val POINT = """{"type":"Point","coordinates":[1,0]}"""
    }
}
