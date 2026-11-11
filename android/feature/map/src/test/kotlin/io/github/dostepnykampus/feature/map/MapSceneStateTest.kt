/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.runtime.mutableStateOf
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.map.data.CampusBounds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Stan sceny mapy V1 bez widoku mapy: dotknięcia konturów i zapis widoku kamery (decyzje 83 i 85, US2.8). */
class MapSceneStateTest {
    private val start = MapCamera(center = GeoPoint(longitude = 17.0, latitude = 51.0), zoom = 16.0)
    private val cameras = mutableListOf<MapCamera>()
    private val opened = mutableListOf<String>()
    private val choices = mutableStateOf(listOf("stary wybór"))
    private var emptyTaps = 0

    private fun state(isCovered: Boolean = false, limits: MapLimits? = null) =
        MapSceneState(
            scene = null,
            camera = start,
            limits = limits,
            isCovered = isCovered,
            choices = choices,
            onCamera = { cameras += it },
        )

    private fun touch(vararg symbols: String) =
        state().touch(symbols = symbols.toList(), onBuilding = { opened += it }, onEmpty = { emptyTaps++ })

    /** Wiersz: dotknięcie dwóch konturów → oba symbole czekają na wybór, żaden budynek się nie otwiera (decyzja 85). */
    @Test
    fun us2_two_outlines_wait() {
        touch("C-3", "C-4")
        assertEquals(listOf("C-3", "C-4"), choices.value)
        assertEquals(emptyList<String>(), opened)
    }

    /** Wiersz: dotknięcie jednego konturu → budynek otwarty od razu, a wcześniejszy wybór zamknięty (decyzja 85). */
    @Test
    fun us2_one_outline_opens_building() {
        touch("C-13")
        assertEquals(listOf("C-13"), opened)
        assertEquals(emptyList<String>(), choices.value)
    }

    /** Wiersz: dotknięcie pustej mapy → zgłoszone aplikacji, wybór bez zmian (US2.8). */
    @Test
    fun us2_8_empty_map_reported() {
        touch()
        assertEquals(1, emptyTaps)
        assertEquals(listOf("stary wybór"), choices.value)
    }

    /** Wiersz: budynek wybrany w arkuszu „Który budynek?” → otwarty, arkusz zamknięty (decyzja 85). */
    @Test
    fun us2_choice_opens_building() {
        state().choose(symbol = "C-4", onBuilding = { opened += it })
        assertEquals(listOf("C-4"), opened)
        assertEquals(emptyList<String>(), choices.value)
    }

    /** Wiersz: widok po geście jest zapisywany, a pod arkuszem V2 nie, więc „Wstecz” wraca do widoku sprzed V2. */
    @Test
    fun d83_camera_kept_uncovered() {
        state(isCovered = true).keep(start)
        assertEquals(emptyList<MapCamera>(), cameras)
        state().keep(start)
        assertEquals(listOf(start), cameras)
    }

    /** Wiersz: przycisk przesuwa kamerę tylko w granicach terenu (NFR-53, BR-G5); bez granic nic nie zapisuje. */
    @Test
    fun brg5_move_stays_inside_area() {
        val far = start.copy(center = GeoPoint(longitude = 50.0, latitude = 51.0))
        state().move(far)
        assertEquals(emptyList<MapCamera>(), cameras)
        val bounds = CampusBounds(west = 16.9, south = 50.9, east = 17.1, north = 51.1)
        val limits = MapLimits(bounds = bounds, widthDp = 360.0, heightDp = 640.0)
        state(limits = limits).move(far)
        assertEquals(listOf(limits.clamp(far)), cameras)
    }
}
