/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import io.github.dostepnykampus.feature.map.camera.TouchSign
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

/**
 * Wybór obiektu przy dotknięciu mapy V1: znak przed konturem budynku, kontury bez znaku w zasięgu.
 *
 * Wymagania: US1.5, US2, NFR-17
 */
class MapTouchTest {
    private val asked = mutableListOf<String>()

    private fun outlinesAt(hit: List<Int>, near: List<Int> = hit): OutlinesQuery =
        OutlinesQuery(
            hit = {
                asked += "hit"
                hit
            },
            near = {
                asked += "near"
                near
            },
        )

    /** Wiersz: V1, znak bariery w zasięgu wygrywa z konturem pod palcem, a kontury nie są sprawdzane. */
    @Test
    fun barrierWinsOverOutline() {
        val touch = chooseTouch(signsNear = { listOf("z1") }, outlines = outlinesAt(hit = listOf(3)))

        assertEquals(MapTouch.Sign(TouchSign.BARRIER, "z1"), touch, "bariera w zasięgu wygrywa")
        assertEquals(emptyList<String>(), asked, "przy trafionym znaku kontury nie są sprawdzane")
    }

    /** Wiersz: V1, bariera i wejście w zasięgu: wygrywa bariera; samo wejście wygrywa z konturem. */
    @Test
    fun entranceBetweenBarrierAndShape() {
        val both = chooseTouch(signsNear = { sign -> listOf(sign.name) }, outlines = outlinesAt(hit = listOf(3)))
        val entrance =
            chooseTouch(
                signsNear = { sign -> if (sign == TouchSign.ENTRANCE) listOf("node/1") else emptyList() },
                outlines = outlinesAt(hit = listOf(3)),
            )

        assertEquals(MapTouch.Sign(TouchSign.BARRIER, "BARRIER"), both, "bariera przed wejściem")
        assertEquals(MapTouch.Sign(TouchSign.ENTRANCE, "node/1"), entrance, "wejście przed konturem")
    }

    /** Wiersz: V1, wejście w zasięgu wygrywa ze znakiem planu. Wymagania: US16.1 */
    @Test
    fun entranceBeforePlanSign() {
        val touch =
            chooseTouch(
                signsNear = { sign -> if (sign == TouchSign.BARRIER) emptyList() else listOf(sign.name) },
                outlines = outlinesAt(hit = emptyList()),
            )

        assertEquals(MapTouch.Sign(TouchSign.ENTRANCE, "ENTRANCE"), touch, "wejście przed znakiem planu")
    }

    /** Wiersz: V1, bez znaku w zasięgu wygrywa kontur pod palcem; kontury obok są liczone na żądanie. */
    @Test
    fun outlineWithoutSign() {
        val outlines = outlinesAt(hit = listOf(3), near = listOf(3, 4))
        val touch = chooseTouch(signsNear = { emptyList() }, outlines = outlines)

        val chosen = assertInstanceOf(MapTouch.Outlines::class.java, touch, "bez znaku wygrywa kontur")
        assertEquals(listOf(3), chosen.hit, "kontur pod palcem")
        assertEquals(listOf("hit"), asked, "kontury obok nie są liczone przed żądaniem")
        assertEquals(listOf(3, 4), chosen.near(), "kontury w zasięgu na żądanie")
    }

    /** Wiersz: V1, dotknięcie pustej mapy nie ma znaku ani konturu. */
    @Test
    fun emptyMapHasNoTarget() {
        val touch = chooseTouch(signsNear = { emptyList() }, outlines = outlinesAt(hit = emptyList()))

        val outlines = assertInstanceOf(MapTouch.Outlines::class.java, touch, "pusta mapa daje puste kontury")
        assertEquals(emptyList<Int>(), outlines.hit, "brak konturu pod palcem")
        assertEquals(emptyList<Int>(), outlines.near(), "brak konturu w zasięgu")
    }

    /** Wiersz: V1, znaki są sprawdzane w kolejności pierwszeństwa i tylko do pierwszego trafienia. */
    @Test
    fun asksSignsInPriorityOrder() {
        val touch =
            chooseTouch(
                signsNear = { sign ->
                    asked += sign.name
                    emptyList()
                },
                outlines = outlinesAt(hit = emptyList()),
            )

        assertInstanceOf(MapTouch.Outlines::class.java, touch, "bez znaku wygrywa kontur")
        assertEquals(TouchSign.entries.map { it.name } + "hit", asked, "znaki po kolei, potem kontur")
    }

    /** Wiersz: V1, znak 20 dp trafiony 24 dp od swojego punktu, a 30 dp już nie. Wymagania: NFR-17 */
    @Test
    fun reachIsSquareAroundSignPoint() {
        val near = listOf(SignPoint(id = "w1", dx = 24f, dy = -24f), SignPoint(id = "w2", dx = 30f, dy = 0f))

        assertEquals(listOf("w1"), signsInReach(under = emptyList(), near = near, reach = 24f), "tylko 48 dp")
    }

    /** Wiersz: V1, obraz znaku większy niż 48 dp jest trafiony w całości. Wymagania: NFR-17 */
    @Test
    fun imageUnderFingerIsHit() {
        val near = listOf(SignPoint(id = "w1", dx = 40f, dy = 0f))

        assertEquals(listOf("w1"), signsInReach(under = listOf("w1"), near = near, reach = 24f), "obraz pod palcem")
    }
}
