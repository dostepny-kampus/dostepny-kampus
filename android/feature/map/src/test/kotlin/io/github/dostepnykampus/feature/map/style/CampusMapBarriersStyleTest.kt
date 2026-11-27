/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.style

import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.core.model.submission.SubmissionFloor
import io.github.dostepnykampus.feature.map.campus.BARRIER_FLOOR
import io.github.dostepnykampus.feature.map.campus.BARRIER_SEVERITY
import io.github.dostepnykampus.feature.map.campus.CampusMapData
import io.github.dostepnykampus.feature.map.campus.ShownPlan
import io.github.dostepnykampus.feature.map.campus.campusMapData
import io.github.dostepnykampus.feature.map.campus.selection
import io.github.dostepnykampus.feature.map.campus.withBarriers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Warstwy barier w stylu mapy V1 na konturze „Budynek testowy” (nie dane PWr). Wymagania: US1.3, NFR-20, NFR-21 */
class CampusMapBarriersStyleTest {
    private val catalog = styleCatalog()
    private val colors = styleColors()
    private val barriers = Severity.entries.map(::barrier)

    /**
     * Wiersz: V1, bariery ze zgłoszeń przyjętych jako znak uciążliwości: kształt, nie kolor.
     *
     * Wymagania: US1.3, NFR-21
     */
    @Test
    fun drawsSignPerSeverity() {
        val style = style(campusMapData(catalog).withBarriers(barriers))
        val points = style.points()
        val severities = points.map { it.jsonObject.obj("properties").text(BARRIER_SEVERITY) }
        assertEquals(Severity.entries.map(Severity::name), severities)
        val signs = style.layer(BARRIERS_LAYER).obj("layout")
        val images = signs.getValue("icon-image").jsonArray.map { it.toString() }
        assertEquals(true, images.containsAll(Severity.entries.map { JsonPrimitive(barrierSignImage(it)).toString() }))
        assertEquals("true", signs.getValue("icon-allow-overlap").toString())
    }

    /** Wiersz: V1, słowo obok znaku od powiększenia 17, bez nakładania; symbole budynków pierwsze. Wymagania: NFR-20 */
    @Test
    fun showsWordFromZoom17() {
        val style = style(campusMapData(catalog).withBarriers(barriers))
        val words = style.layer(BARRIER_WORDS_LAYER)
        assertEquals(BARRIER_WORD_ZOOM, words.getValue("minzoom").jsonPrimitive.int)
        assertNull(words.obj("layout")["icon-allow-overlap"])
        assertEquals("true", words.obj("layout").getValue("symbol-avoid-edges").toString())
        val order = style.getValue("layers").jsonArray.map { it.jsonObject.text("id") }
        assertEquals(true, order.indexOf(BARRIER_WORDS_LAYER) < order.indexOf(BARRIERS_LAYER))
        assertEquals(true, order.indexOf(BARRIERS_LAYER) < order.indexOf("podpisy"))
    }

    /** Wymagania: US1.5, NFR-21 */
    @Test
    fun pinsChosenBarrierWithoutChangingItsSign() {
        val data = campusMapData(catalog).withBarriers(barriers)
        val chosen = barriers.first().id
        val plain = Json.parseToJsonElement(campusMapStyle(data, data.selection(), colors)).jsonObject
        val marked = data.selection().copy(barrier = chosen, barrierCaption = "Schody przy wejściu")
        val style = Json.parseToJsonElement(campusMapStyle(data, marked, colors)).jsonObject
        val order = style.getValue("layers").jsonArray.map { it.jsonObject.text("id") }
        assertEquals(false, "bariera_wybrana" in order)
        assertEquals(true, order.indexOf(BARRIERS_LAYER) < order.indexOf(ENTRANCE_PIN_LAYER))
        assertEquals(plain.layer(BARRIERS_LAYER).obj("layout"), style.layer(BARRIERS_LAYER).obj("layout"))
        val pin = style.layer(ENTRANCE_PIN_LAYER).obj("layout")
        assertEquals(JsonPrimitive("bottom"), pin["icon-anchor"])
        assertEquals(JsonPrimitive("top"), style.layer(ENTRANCE_CAPTION_LAYER).obj("layout")["icon-anchor"])
        val point = style.obj("sources").obj(CHOSEN_SOURCE).obj("data").getValue("features").jsonArray.single().jsonObject
        assertEquals(style.points().first().jsonObject["geometry"], point["geometry"])
        assertEquals("Schody przy wejściu", point.obj("properties").text("label"))
    }

    /** Wymagania: US1.5, US16.1, BR-14 */
    @Test
    fun keepsUnknownFloorSignAndHidesPinForAnotherFloor() {
        val unknown = barriers.first()
        val known = unknown.copy(id = "known", floor = SubmissionFloor(floorId = "P0"))
        val data = campusMapData(catalog).withBarriers(listOf(unknown, known))
        val plan = ShownPlan(building = "C-13", floors = emptyList(), floor = "P1", rooms = emptyMap())
        val selected = data.selection().copy(barrier = unknown.id, barrierCaption = "Próg", plan = plan)

        val look = selectionLook(data, selected)
        assertEquals(BARRIER_NO_FLOOR_DP / 2.0 + 2, look.chosenPoint.barrierCaptionOffset)
        val hidden = selectionLook(data, selected.copy(barrier = known.id))
        assertEquals(emptyList<JsonObject>(), hidden.chosenPoint.data.getValue("features").jsonArray)
    }

    /**
     * Wiersz: V1, przy planie budynku bariera tego budynku bez piętra bierze znak z „?”; bez planu znak się nie
     * zmienia.
     *
     * Wymagania: US16.1, BR-14
     */
    @Test
    fun picksUnknownFloorSigns() {
        val plain = barrierSignsImage(planBuilding = null).toString()
        assertEquals(false, plain.contains("bez_pietra"), "bez planu nie ma znaków z „?”")
        val planned = barrierSignsImage(planBuilding = "C-13").jsonArray
        assertEquals("\"case\"", planned.first().toString(), "wybór obrazu zależy od warunku")
        val condition = planned[1].toString()
        assertEquals(true, condition.contains("\"C-13\""), "warunek dotyczy budynku z planem")
        val noFloor = """["!",["has","$BARRIER_FLOOR"]]"""
        assertEquals(true, condition.contains(noFloor), "warunek dotyczy bariery bez piętra")
        val unknown = planned[2].toString()
        val named = Severity.entries.map { JsonPrimitive(barrierNoFloorImage(it)).toString() }
        assertEquals(true, named.all(unknown::contains), "przy planie znaki z „?”")
        assertEquals(plain, planned[3].toString(), "pozostałe bariery mają zwykłe znaki")
    }

    /** Wiersz: V1 bez pobranych barier działa bez znaków barier. Wymagania: NFR-11 */
    @Test
    fun leavesLayerEmptyWithoutData() {
        val shown = campusMapData(catalog).withBarriers(barriers).withBarriers(emptyList())
        assertEquals(0, style(shown).points().size)
    }

    private fun style(data: CampusMapData): JsonObject =
        Json.parseToJsonElement(campusMapStyle(data, data.selection(), colors)).jsonObject

    private fun barrier(severity: Severity) =
        AcceptedBarrier(
            id = severity.name,
            location = STYLE_ORIGIN,
            severity = severity,
            type = null,
            buildingSymbol = "C-13",
            verifiedAt = null,
        )

    private fun JsonObject.layer(id: String): JsonObject =
        getValue("layers").jsonArray.map { it.jsonObject }.single { it.text("id") == id }

    private fun JsonObject.points(): JsonArray {
        val source = obj("sources").obj(BARRIERS_SOURCE)
        return source.obj("data").getValue("features").jsonArray
    }

    private fun JsonObject.obj(key: String): JsonObject = getValue(key).jsonObject

    private fun JsonObject.text(key: String): String = getValue(key).jsonPrimitive.content
}
