/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.style

import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.feature.map.campus.BARRIER_ENTRANCE
import io.github.dostepnykampus.feature.map.campus.BARRIER_OFFSET
import io.github.dostepnykampus.feature.map.campus.BARRIER_SEVERITY
import io.github.dostepnykampus.feature.map.campus.ENTRANCE_BARRIER_DP
import io.github.dostepnykampus.feature.map.campus.MapSelection
import io.github.dostepnykampus.feature.map.campus.SelectedEntrance
import io.github.dostepnykampus.feature.map.campus.campusMapData
import io.github.dostepnykampus.feature.map.campus.selection
import io.github.dostepnykampus.feature.map.campus.withBarriers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.math.roundToInt

/**
 * Znak bariery zgłoszonej przy wejściu na V1 stoi obok widocznego znaku wejścia, na zewnątrz konturu, a gdy znaku
 * wejścia nie widać, w punkcie wejścia; kilka barier przy jednym wejściu daje jeden znak. Kontur „Budynek testowy”
 * (nie dane PWr).
 *
 * Wymagania: US1.3, US2.2, BR-8, NFR-17
 */
class EntranceBarrierSignTest {
    private val data =
        campusMapData(styleCatalog()).copy(
            entrancePoints = mapOf("w1" to GeoPoint(longitude = 17.5, latitude = 51.25)),
            buildingOutlines =
                mapOf("C-13" to outline(17.0 to 51.0, 17.5 to 51.0, 17.5 to 51.5, 17.0 to 51.5)),
        )

    /**
     * Wiersz: V1, „Utrudnienie” i „Blokada” przy wejściu na wschodniej ścianie i bariera bez wejścia → dwa znaki:
     *   „Blokada” przesunięta o 30 dp na wschód i bariera bez wejścia bez przesunięcia.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Znak bariery przy wejściu stoi obok znaku wejścia
     */
    @Test
    fun putsOneSignBesideEntrance() {
        val shown =
            data.withBarriers(
                listOf(
                    barrier("a", Severity.HINDRANCE, entrance = "w1"),
                    barrier("b", Severity.BLOCKING, entrance = "w1"),
                    barrier("c", Severity.ONLY_WITH_HELP, entrance = null),
                ),
            )
        val properties =
            shown.barriers.jsonObject
                .getValue("features")
                .jsonArray
                .map { it.jsonObject.getValue("properties").jsonObject }
        assertEquals(
            listOf("ONLY_WITH_HELP" to null, "BLOCKING" to listOf(30, 0)),
            properties.map { it.text(BARRIER_SEVERITY) to it.offset() },
            "Przesunięty jest tylko znak bariery przy wejściu",
        )
        assertEquals("w1", properties.last().text(BARRIER_ENTRANCE), "Identyfikator wejścia przy znaku")
    }

    /**
     * Wiersz: V1, „Blokada” przy wejściu w zagłębieniu budynku w kształcie litery L, na ścianie zwróconej na
     *   wschód → znak przesunięty o 30 dp na wschód, poza kontur, a nie na północ od środka prostokąta.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Znak bariery przy wejściu stoi obok znaku wejścia
     */
    @Test
    fun putsSignOutOfConcaveNotch() {
        val corners = listOf(17.0 to 51.0, 17.5 to 51.0, 17.5 to 51.25, 17.25 to 51.25, 17.25 to 51.5, 17.0 to 51.5)
        val concave =
            data.copy(
                entrancePoints = mapOf("w1" to GeoPoint(longitude = 17.25, latitude = 51.4)),
                buildingOutlines = mapOf("C-13" to outline(*corners.toTypedArray())),
            )
        val shown = concave.withBarriers(listOf(barrier("b", Severity.BLOCKING, entrance = "w1")))
        val sign =
            shown.barriers.jsonObject
                .getValue("features")
                .jsonArray
                .single()
                .jsonObject
                .getValue("properties")
                .jsonObject
        assertEquals(
            listOf(30, 0),
            sign.offset(),
            "Znak bariery w zagłębieniu konturu nie stoi na zewnątrz ściany wejścia",
        )
    }

    /**
     * Wiersz: V1 bez zaznaczenia, znak i wyróżniony znak bariery biorą przesunięcie z punktu od powiększenia, od
     *   którego widać znaki wejść, a niżej stoją na punkcie.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Znak bariery przy wejściu stoi obok znaku wejścia
     */
    @Test
    fun offsetsFromEntranceZoom() {
        val zero = """["literal",[0.0,0.0]]"""
        val moved = """["coalesce",["get","$BARRIER_OFFSET"],$zero]"""
        val expected = """["step",["zoom"],$zero,17.0,$moved]"""
        assertEquals(listOf(expected), signOffsets(data.selection()), "Przesunięcie od progu znaków wejść")
    }

    /**
     * Wiersz: V1 z zaznaczonym budynkiem C-13: jego znaki wejść widać przy każdym powiększeniu, więc znak bariery
     *   przy jego wejściu stoi obok przy każdym, a przy wejściach innych budynków w punkcie wejścia.
     */
    @Test
    fun offsetsAtSelectedBuilding() {
        val zero = """["literal",[0.0,0.0]]"""
        val moved = """["coalesce",["get","$BARRIER_OFFSET"],$zero]"""
        val match = """["match",["to-string",["get","building"]],["C-13"],$moved,$zero]"""
        val expected = """["step",["zoom"],$zero,0.0,$match]"""
        assertEquals(
            listOf(expected),
            signOffsets(data.selection(building = "C-13")),
            "Przy zaznaczonym budynku przesunięcie tylko przy jego wejściach",
        )
    }

    @Test
    fun keepsBarrierBesidePin() {
        val entrance =
            SelectedEntrance(
                id = "w1",
                location = data.entrancePoints.getValue("w1"),
                label = "Wejście",
                buildingSymbol = "C-13",
            )
        val zero = """["literal",[0.0,0.0]]"""
        val still = """["literal",[20.0,0.0]]"""
        val sign = """["literal",[48.0,0.0]]"""
        val word = """["literal",[68.0,0.0]]"""
        val pin = """["==",["get","entrance"],"w1"]"""
        val selection = data.selection(entrance = entrance)
        val signMoves = """["coalesce",["get","$BARRIER_OFFSET"],$zero]"""
        val wordMoves = """["coalesce",["get","word_offset"],$still]"""
        val expectedSign = """["step",["zoom"],["case",$pin,$sign,$zero],17.0,["case",$pin,$sign,$signMoves]]"""
        val expectedWord = """["step",["zoom"],["case",$pin,$word,$still],17.0,["case",$pin,$word,$wordMoves]]"""
        assertEquals(listOf(expectedSign), signOffsets(selection), "Znak obok pinezki")
        assertEquals(listOf(expectedWord), iconOffsets(selection, listOf(BARRIER_WORDS_LAYER)), "Słowo obok znaku")
        val beside = selectionLook(data, selection).besideEntrance
        assertEquals(48.0, beside.pinnedOffset("w1"), "Przesunięcie używane przy dotknięciu")
        assertNull(beside.pinnedOffset("w2"), "Inne wejście zachowuje zwykłe przesunięcie")
    }

    /** Wiersz: V1, znak bariery stoi obok znaku wejścia dokładnie tam, gdzie ten znak widać. */
    @Test
    fun besideWhereEntranceShows() {
        val all = selectionLook(data, data.selection())
        val chosen = selectionLook(data, data.selection(building = "C-13"))
        val cases =
            listOf(
                all.besideEntrance.isBeside(zoom = all.entrancesMinZoom - 0.1, building = "C-13"),
                all.besideEntrance.isBeside(zoom = all.entrancesMinZoom, building = "C-13"),
                chosen.besideEntrance.isBeside(zoom = 12.0, building = "C-13"),
                chosen.besideEntrance.isBeside(zoom = 18.0, building = "A-1"),
                chosen.besideEntrance.isBeside(zoom = 18.0, building = null),
            )
        assertEquals(listOf(false, true, true, false, false), cases, "Obok tylko przy widocznym znaku wejścia")
    }

    /** Wiersz: V1, znaki wejścia i bariery stykają się: odstęp środków to połowy obu znaków. Wymagania: NFR-17 */
    @Test
    fun signsTouch() {
        assertEquals(
            ENTRANCE_MARK_DP / 2.0 + BARRIER_SIGN_DP / 2.0,
            ENTRANCE_BARRIER_DP,
            "Odstęp znaku bariery od punktu wejścia to połowa znaku wejścia i połowa znaku bariery",
        )
    }

    /** Pinezka dzieli przesunięcie znaku bariery przy wejściu. Wymagania: US1.5, US2.2, NFR-21 */
    @Test
    fun offsetsPinBesideEntrance() {
        val shown = data.withBarriers(listOf(barrier("b", Severity.BLOCKING, entrance = "w1")))
        val selected = shown.selection().copy(barrier = "b", barrierCaption = "Schody przy wejściu")
        val look = selectionLook(shown, selected)
        val properties =
            look.chosenPoint.data
                .getValue("features")
                .jsonArray
                .single()
                .jsonObject
                .getValue("properties")
                .jsonObject
        val sign = properties.getValue(BARRIER_OFFSET).jsonArray
        val label = properties.getValue(BARRIER_CAPTION_OFFSET).jsonArray
        assertEquals(look.besideEntrance.signOffset(), pinOffset(look), "pinezka ma przesunięcie znaku")
        assertEquals(sign[0], label[0], "podpis jest w osi znaku")
        val gap = label[1].jsonPrimitive.double - sign[1].jsonPrimitive.double
        assertEquals(BARRIER_SIGN_DP / 2.0 + 2, gap, 1e-9, "podpis jest poniżej znaku")
    }

    private fun signOffsets(selection: MapSelection): List<String> = iconOffsets(selection, listOf(BARRIERS_LAYER))

    private fun iconOffsets(selection: MapSelection, ids: List<String>): List<String> {
        val style = Json.parseToJsonElement(campusMapStyle(data, selection, styleColors())).jsonObject
        val layers = style.getValue("layers").jsonArray.map { it.jsonObject }
        return ids.map { id ->
            layers
                .single { it.text("id") == id }
                .getValue("layout")
                .jsonObject
                .getValue("icon-offset")
                .toString()
        }
    }

    private fun barrier(id: String, severity: Severity, entrance: String?) =
        AcceptedBarrier(
            id = id,
            location = GeoPoint(longitude = 17.5, latitude = 51.25),
            severity = severity,
            type = null,
            buildingSymbol = "C-13",
            verifiedAt = null,
            entranceId = entrance,
        )

    private fun JsonObject.text(key: String): String = getValue(key).jsonPrimitive.content

    private fun JsonObject.offset(): List<Int>? =
        get(BARRIER_OFFSET)?.run { jsonArray.map { it.jsonPrimitive.double.roundToInt() } }

    private fun outline(vararg corners: Pair<Double, Double>): GeoPolygon =
        GeoPolygon(listOf((corners.toList() + corners.first()).map { (x, y) -> GeoPoint(longitude = x, latitude = y) }))
}
