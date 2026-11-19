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
import io.github.dostepnykampus.feature.map.barrier.BARRIER_OFFSET
import io.github.dostepnykampus.feature.map.barrier.BARRIER_SEVERITY
import io.github.dostepnykampus.feature.map.barrier.withBarriers
import io.github.dostepnykampus.feature.map.campus.campusMapData
import io.github.dostepnykampus.feature.map.campus.selection
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.roundToInt

/**
 * Znak bariery zgłoszonej przy wejściu na V1 stoi obok znaku wejścia, na zewnątrz konturu, a kilka barier przy
 * jednym wejściu daje jeden znak. Kontur „Budynek testowy” (nie dane PWr).
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
     *   „Blokada” przesunięta o 48 dp na wschód i bariera bez wejścia bez przesunięcia.
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
            listOf("ONLY_WITH_HELP" to null, "BLOCKING" to listOf(48, 0)),
            properties.map { it.text(BARRIER_SEVERITY) to it.offset() },
            "Przesunięty jest tylko znak bariery przy wejściu",
        )
    }

    /**
     * Wiersz: V1, „Blokada” przy wejściu w zagłębieniu budynku w kształcie litery L, na ścianie zwróconej na
     *   wschód → znak przesunięty o 48 dp na wschód, poza kontur, a nie na północ od środka prostokąta.
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
            listOf(48, 0),
            sign.offset(),
            "Znak bariery w zagłębieniu konturu nie stoi na zewnątrz ściany wejścia",
        )
    }

    /** Wiersz: V1, znak i wyróżniony znak bariery biorą przesunięcie z punktu, a bez niego stoją na punkcie. */
    @Test
    fun offsetsSignsByProperty() {
        val style = Json.parseToJsonElement(campusMapStyle(data, data.selection(), styleColors())).jsonObject
        val layers = style.getValue("layers").jsonArray.map { it.jsonObject }
        val offsets =
            listOf(BARRIERS_LAYER, CHOSEN_BARRIER_LAYER).map { id ->
                layers
                    .single { it.text("id") == id }
                    .getValue("layout")
                    .jsonObject
                    .getValue("icon-offset")
                    .toString()
            }
        val expected = """["coalesce",["get","$BARRIER_OFFSET"],["literal",[0.0,0.0]]]"""
        assertEquals(listOf(expected, expected), offsets, "Obie warstwy znaków biorą przesunięcie z punktu")
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
