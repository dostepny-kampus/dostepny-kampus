/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.camera

import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.model.CampusBounds
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.map.campus.campusMapData
import io.github.dostepnykampus.feature.map.campus.selection
import io.github.dostepnykampus.feature.map.style.campusMapStyle
import io.github.dostepnykampus.feature.map.style.styleColors
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.PI
import kotlin.math.pow

private const val VIEW_WIDTH = 360.0
private const val VIEW_HEIGHT = 696.0
private const val WORLD_DP = 512.0
private const val DEGREES = 360.0
private const val STEPS = 100
private const val DP_DELTA = 0.5
private val BOUNDARY_WORDS = listOf("teren", "granic", "wyblak", "mask", "fade")

private fun pwrBuildings(): Path =
    generateSequence(Path.of("").toAbsolutePath(), Path::getParent)
        .map { it.resolve("data/buildings/pwr-buildings.geojson") }
        .first(Files::exists)

/** Najmniejsze powiększenie mapy V1 na terenie „Wittigowo” z prawdziwych danych PWr na telefonie 360 × 696 dp. */
class PwrAreaFitTest {
    private val data =
        campusMapData(
            runBlocking {
                BuildingCatalogRepository(EmptyCoroutineContext) { Files.readString(pwrBuildings()) }.catalog()
            },
        )
    private val area = data.areas.single { it.name == "Wittigowo" }.bounds
    private val limits = MapLimits(area, widthDp = VIEW_WIDTH, heightDp = VIEW_HEIGHT)

    private fun CampusBounds.widthDp(zoom: Double): Double = (east - west) / DEGREES * WORLD_DP * 2.0.pow(zoom)

    private fun CampusBounds.heightDp(zoom: Double): Double =
        (mercatorY(north) - mercatorY(south)) / (2 * PI) * WORLD_DP * 2.0.pow(zoom)

    private fun reach(from: MapCamera, direction: PanDirection): GeoPoint =
        (1..STEPS).fold(from) { view, _ -> limits.clamp(view.moved(direction)) }.center

    private fun travelDp(from: MapCamera, isHorizontal: Boolean): Double {
        val world = WORLD_DP * 2.0.pow(from.zoom)
        return if (isHorizontal) {
            val span = reach(from, PanDirection.RIGHT).longitude - reach(from, PanDirection.LEFT).longitude
            span / DEGREES * world
        } else {
            val span =
                mercatorY(reach(from, PanDirection.UP).latitude) - mercatorY(reach(from, PanDirection.DOWN).latitude)
            span / (2 * PI) * world
        }
    }

    private fun styleNames(): List<String> {
        val root = Json.parseToJsonElement(campusMapStyle(data, data.selection(area = "Wittigowo"), styleColors()))
        val layers = root.jsonObject.getValue("layers").jsonArray
        val ids = layers.map { layer -> layer.jsonObject.text("id") }
        val sources = root.jsonObject.getValue("sources")
        return ids + sources.jsonObject.keys
    }

    private fun JsonObject.text(key: String): String = getValue(key).jsonPrimitive.content

    /**
     * Wiersz: V1 na terenie „Wittigowo”, „Pomniejsz” do końca → prostokąt terenu wypełnia mapę w krótszym wymiarze,
     * strzałki dochodzą tylko do brzegu prostokąta wzdłuż dłuższego, a styl nie ma granicy terenu ani wyblakłego
     * podkładu.
     * Scenariusz: przegladanie-mapy.feature:
     *   Przy najmniejszym powiększeniu teren wypełnia mapę w krótszym wymiarze
     *
     * Wymagania: US1.7, BR-G5, NFR-39
     */
    @Test
    fun fillsShorterSideOfArea() {
        val center = GeoPoint(longitude = (area.west + area.east) / 2, latitude = (area.south + area.north) / 2)
        val smallest = (1..STEPS).fold(MapCamera(center, START_ZOOM)) { view, _ -> limits.clamp(view.zoomedOut()) }
        assertEquals(limits.minZoom, smallest.zoom, 0.0, "„Pomniejsz” do końca daje najmniejsze powiększenie")
        val wide = area.widthDp(smallest.zoom) - VIEW_WIDTH
        val tall = area.heightDp(smallest.zoom) - VIEW_HEIGHT
        assertEquals(0.0, minOf(a = wide, b = tall), DP_DELTA, "teren wypełnia mapę w krótszym wymiarze")
        assertTrue(wide >= -DP_DELTA && tall >= -DP_DELTA, "teren nie jest mniejszy niż mapa: $wide, $tall")
        assertEquals(wide, travelDp(smallest, isHorizontal = true), DP_DELTA, "w poziomie tylko do brzegu terenu")
        assertEquals(tall, travelDp(smallest, isHorizontal = false), DP_DELTA, "w pionie tylko do brzegu terenu")
        val names = styleNames()
        assertTrue(names.none { id -> BOUNDARY_WORDS.any(id::contains) }, "granica terenu albo wyblaknięcie: $names")
        assertEquals(area, limits.tileBounds, "podkład tylko w prostokącie terenu")
    }
}
