/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.runtime.mutableStateOf
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.map.camera.FreeMap
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.camera.MapInset
import io.github.dostepnykampus.feature.map.camera.MapLimits
import io.github.dostepnykampus.feature.map.camera.START_ZOOM
import io.github.dostepnykampus.feature.map.camera.byControl
import io.github.dostepnykampus.feature.map.camera.zoomedOut
import io.github.dostepnykampus.feature.map.campus.CampusBounds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Stan sceny mapy V1 bez widoku mapy: dotknięcia konturów i zapis widoku kamery. Wymagania: US2, US2.8 */
class MapSceneStateTest {
    private val start = MapCamera(center = GeoPoint(longitude = 17.0, latitude = 51.0), zoom = 16.0)
    private val cameras = mutableListOf<MapCamera>()
    private val cardCameras = mutableListOf<MapCamera>()
    private val opened = mutableListOf<String>()
    private val choices = mutableStateOf(listOf("stary wybór"))
    private var emptyTaps = 0
    private val landing = FreeMap(widthDp = 411.0, heightDp = 600.0, endDp = 160.0, marginDp = 24.0)

    private fun state(isCovered: Boolean = false, limits: MapLimits? = null) =
        MapSceneState(
            scene = null,
            shown = SceneCamera(move = start.byControl(), inset = MapInset(), landing = landing),
            limits = limits,
            cover = mapCover(isCovered, hasControls = true),
            choices = choices,
            onCamera = cameraKeeper(isCovered, onMap = { cameras += it }, onCard = { cardCameras += it }),
        )

    /**
     * Wiersz: V1, wybór terenu z listy zapisuje widok na wszystkie budynki terenu, nie bliżej niż powiększenie
     * startowe.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Po wyborze terenu widać wszystkie jego budynki
     *
     * Wymagania: US1.7, BR-G5
     */
    @Test
    fun landsOnChosenArea() {
        val building = CampusBounds(west = 17.0599, south = 51.1099, east = 17.0601, north = 51.1101)
        state().land(building)
        assertEquals(START_ZOOM, cameras.single().zoom, "budynek mały: powiększenie startowe")
        assertEquals(51.11, cameras.single().center.latitude, 1e-9, "budynek na środku w pionie")
    }

    private fun touch(vararg symbols: String) =
        state().touch(symbols = symbols.toList(), onBuilding = { opened += it }, onEmpty = { emptyTaps++ })

    /**
     * Scenariusz „Przy niskiej mapie nad kartą nie ma przycisków mapy”: bez arkusza przyciski są zawsze,
     * nad arkuszem tylko wtedy, gdy się mieszczą.
     *
     * Wymagania: US2.8, NFR-53
     */
    @Test
    fun decidesButtonsByCover() {
        val covers =
            listOf(true to true, true to false, false to false).map { (isCovered, hasRoom) ->
                mapCover(isCovered, hasControls = hasRoom)
            }
        assertEquals(listOf(MapCover.SHEET, MapCover.SHEET_WITHOUT_CONTROLS, MapCover.NONE), covers)
        assertEquals(true to true, state(isCovered = true).run { isCovered to hasControls })
        assertEquals(false to true, state().run { isCovered to hasControls })
    }

    /** Wiersz: dotknięcie dwóch konturów → oba symbole czekają na wybór, żaden budynek się nie otwiera. */
    @Test
    fun waitsOnTwoOutlines() {
        touch("C-3", "C-4")
        assertEquals(listOf("C-3", "C-4"), choices.value)
        assertEquals(emptyList<String>(), opened)
    }

    /** Wiersz: dotknięcie jednego konturu → budynek otwarty od razu, a wcześniejszy wybór zamknięty. */
    @Test
    fun opensBuildingOnOneOutline() {
        touch("C-13")
        assertEquals(listOf("C-13"), opened)
        assertEquals(emptyList<String>(), choices.value)
    }

    /** Wiersz: dotknięcie pustej mapy → zgłoszone aplikacji, wybór bez zmian. Wymagania: US2.8 */
    @Test
    fun reportsEmptyMapTouch() {
        touch()
        assertEquals(1, emptyTaps)
        assertEquals(listOf("stary wybór"), choices.value)
    }

    /** Wiersz: budynek wybrany w arkuszu „Który budynek?” → otwarty, arkusz zamknięty. */
    @Test
    fun opensChosenBuilding() {
        state().choose(symbol = "C-4", onBuilding = { opened += it })
        assertEquals(listOf("C-4"), opened)
        assertEquals(emptyList<String>(), choices.value)
    }

    /**
     * Wiersz: V1 z małą kartą V2, osoba przesunęła mapę → jej widok jest widokiem karty i zostaje po zamknięciu
     * karty.
     *
     * Wymagania: US2.8
     */
    @Test
    fun keepsMovedView() {
        val moved = start.copy(zoom = start.zoom + 1)
        state(isCovered = true).keep(moved)
        assertEquals(listOf(moved), cardCameras)
        assertEquals(listOf(moved), cameras)
    }

    /**
     * Wiersz: V1 z małą kartą V2, osoba nie ruszyła mapy → widok V1 bez zmian, więc wraca widok sprzed karty;
     * gest bez karty zapisuje tylko widok V1.
     */
    @Test
    fun returnsUntouchedView() {
        state(isCovered = true).touch(symbols = listOf("C-13"), onBuilding = {}, onEmpty = {})
        assertEquals(emptyList<MapCamera>(), cameras + cardCameras)
        state().keep(start)
        assertEquals(listOf(start), cameras)
        assertEquals(emptyList<MapCamera>(), cardCameras)
    }

    /**
     * Scenariusz „Przy karcie budynek jest cały widoczny, a mapą sterują przyciski”: przycisk nad małą kartą
     * zmienia widok karty w granicach terenu i zostaje po zamknięciu karty.
     *
     * Wymagania: US2.8, NFR-53
     */
    @Test
    fun movesCardViewWithButton() {
        val bounds = CampusBounds(west = 16.9, south = 50.9, east = 17.1, north = 51.1)
        val limits = MapLimits(bounds = bounds, widthDp = 360.0, heightDp = 640.0)
        state(isCovered = true, limits = limits).move(start.zoomedOut())
        assertEquals(listOf(limits.clamp(start.zoomedOut())), cardCameras)
        assertEquals(cardCameras, cameras)
    }

    /**
     * Wiersz: przycisk przesuwa kamerę tylko w granicach terenu; bez granic nic nie zapisuje.
     *
     * Wymagania: NFR-53, BR-G5
     */
    @Test
    fun keepsMoveInsideArea() {
        val far = start.copy(center = GeoPoint(longitude = 50.0, latitude = 51.0))
        state().move(far)
        assertEquals(emptyList<MapCamera>(), cameras)
        val bounds = CampusBounds(west = 16.9, south = 50.9, east = 17.1, north = 51.1)
        val limits = MapLimits(bounds = bounds, widthDp = 360.0, heightDp = 640.0)
        state(limits = limits).move(far)
        assertEquals(listOf(limits.clamp(far)), cameras)
    }
}
