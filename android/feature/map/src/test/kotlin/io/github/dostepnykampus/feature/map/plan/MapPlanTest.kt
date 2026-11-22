/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.plan

import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
import io.github.dostepnykampus.core.data.barrier.AcceptedBarriers
import io.github.dostepnykampus.core.data.building.ServerEntrances
import io.github.dostepnykampus.core.data.room.FloorPlans
import io.github.dostepnykampus.core.model.GeoMultiPolygon
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.model.building.Building
import io.github.dostepnykampus.core.model.building.BuildingCatalog
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.core.model.building.Floor
import io.github.dostepnykampus.core.model.building.FloorList
import io.github.dostepnykampus.core.model.room.RoomList
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.campus.ShownPlan
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Plan piętra przy widoku mapy: budynek, piętro na start, pamięć piętra i pobranie planu.
 *
 * Wymagania: US16.1, BR-25, NFR-10
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MapPlanTest {
    @Test
    fun showsGroundFloorAtCenter() =
        runTest(UnconfinedTestDispatcher()) {
            val plan = MapPlan(Plans(), entrances())
            val shown = collected(plan)
            plan.camera(MapCamera(center = INSIDE_A, zoom = 18.0))
            assertEquals(
                listOf("A", "P0", "P-1,P0,P1"),
                shown.value?.run { listOf(building, floor, rooms.keys.sorted().joinToString(",")) },
                "Budynek w środku mapy nie pokazał parteru z pomieszczeniami wszystkich pięter",
            )
        }

    @Test
    fun hidesPlanWithMap() =
        runTest(UnconfinedTestDispatcher()) {
            val plan = MapPlan(Plans(), entrances())
            val shown = collected(plan)
            plan.camera(MapCamera(center = INSIDE_A, zoom = 18.0))
            val before = shown.value?.building
            plan.hide()
            assertEquals(
                listOf("A", null),
                listOf(before, shown.value?.building),
                "Plan budynku został po zejściu mapy z ekranu",
            )
        }

    @Test
    fun remembersChosenFloor() =
        runTest(UnconfinedTestDispatcher()) {
            val plan = MapPlan(Plans(), entrances())
            val shown = collected(plan)
            plan.camera(MapCamera(center = INSIDE_A, zoom = 18.0))
            plan.choose(building = "A", floor = "P1")
            plan.camera(MapCamera(center = INSIDE_A, zoom = 15.0))
            plan.camera(MapCamera(center = INSIDE_A, zoom = 18.0))
            assertEquals("P1", shown.value?.floor, "Powrót do budynku nie pokazał ostatnio wybranego piętra")
        }

    @Test
    fun showsFloorOfFocusOnce() =
        runTest(UnconfinedTestDispatcher()) {
            val plan = MapPlan(Plans(), entrances())
            val shown = collected(plan)
            val focus = MapFocus.Building(symbol = "A", command = 1L, floor = "P-1")
            plan.show(focus)
            plan.camera(MapCamera(center = INSIDE_A, zoom = 18.0))
            val fromFocus = shown.value?.floor
            plan.choose(building = "A", floor = "P1")
            plan.show(focus)
            assertEquals(
                listOf("P-1", "P1"),
                listOf(fromFocus, shown.value?.floor),
                "„Pokaż na planie” nie pokazał piętra albo to samo zaznaczenie cofnęło wybrane piętro",
            )
        }

    /** Sala z zaznaczenia jest wyróżniona na planie swojego budynku, a zaznaczenie bez sali zdejmuje wyróżnienie. */
    @Test
    fun highlightsRoomOfFocus() =
        runTest(UnconfinedTestDispatcher()) {
            val plan = MapPlan(Plans(), entrances())
            val shown = collected(plan)
            plan.show(MapFocus.Building(symbol = "A", command = 1L, floor = "P1", room = "7"))
            plan.camera(MapCamera(center = INSIDE_A, zoom = 18.0))
            val chosen = shown.value?.room
            plan.show(MapFocus.Building(symbol = "A", command = 2L))
            assertEquals(listOf("7", null), listOf(chosen, shown.value?.room), "Sala z zaznaczenia nie jest wyróżniona")
        }

    /** Sala innego budynku nie jest wyróżniona na pokazanym planie, nawet gdy identyfikatory się zgadzają. */
    @Test
    fun skipsRoomOfOtherBuilding() =
        runTest(UnconfinedTestDispatcher()) {
            val plan = MapPlan(Plans(), entrances())
            val shown = collected(plan)
            plan.show(MapFocus.Building(symbol = "B", command = 1L, room = "7"))
            plan.camera(MapCamera(center = INSIDE_A, zoom = 18.0))
            assertEquals(
                listOf("A", null),
                listOf(shown.value?.building, shown.value?.room),
                "Plan budynku A wyróżnił salę budynku B",
            )
        }

    @Test
    fun hidesPlanOfBuildingWithoutCopy() =
        runTest(UnconfinedTestDispatcher()) {
            val plan = MapPlan(Plans(stored = emptySet()), entrances())
            val shown = collected(plan)
            plan.camera(MapCamera(center = INSIDE_A, zoom = 18.0))
            assertEquals(null, shown.value, "Budynek bez kopii planu dostał plan")
        }

    @Test
    fun downloadsPlanOnceAfterSuccess() =
        runTest(UnconfinedTestDispatcher()) {
            val plans = Plans()
            val plan = MapPlan(plans, entrances())
            val shown = collected(plan)
            plan.camera(MapCamera(center = INSIDE_A, zoom = 18.0))
            plan.camera(MapCamera(center = INSIDE_A, zoom = 15.0))
            plan.camera(MapCamera(center = INSIDE_A, zoom = 18.0))
            assertEquals(
                listOf("A", "A"),
                plans.downloads + listOfNotNull(shown.value?.building),
                "Plan budynku pobrany ponownie po udanym pobraniu albo niepokazany",
            )
        }

    @Test
    fun startsOnEntranceFloor() =
        runTest(UnconfinedTestDispatcher()) {
            val plan = MapPlan(Plans(), entrances(floor = "P1"))
            val shown = collected(plan)
            plan.camera(MapCamera(center = INSIDE_A, zoom = 18.0))
            assertEquals("P1", shown.value?.floor, "Plan nie zaczął się na piętrze zalecanego wejścia")
        }

    @Test
    fun startsOnUnblockedEntrance() =
        runTest(UnconfinedTestDispatcher()) {
            val barriers = synced(listOf(barrierAt(entrance = "w1", severity = Severity.BLOCKING)))
            val plan = MapPlan(Plans(), twoEntrances(), barriers)
            val shown = collected(plan)
            plan.camera(MapCamera(center = INSIDE_A, zoom = 18.0))
            assertEquals(
                "P1",
                shown.value?.floor,
                "Plan startował na piętrze wejścia z blokadą, a karta wybiera drugie wejście",
            )
        }

    @Test
    fun startsOnGroundForUnknownFloor() =
        runTest(UnconfinedTestDispatcher()) {
            val plan = MapPlan(Plans(), entrances(floor = "P7"))
            val shown = collected(plan)
            plan.camera(MapCamera(center = INSIDE_A, zoom = 18.0))
            assertEquals("P0", shown.value?.floor, "Piętro wejścia spoza planu nie dało parteru")
        }

    @Test
    fun hidesPlanWhenBasicSelected() =
        runTest(UnconfinedTestDispatcher()) {
            val plan = MapPlan(Plans(setOf("A")), entrances())
            val shown = collected(plan, CLOSE_CATALOG)
            plan.moveTo(INSIDE_A)
            val initial = shown.value?.building
            plan.select("B-8")
            val selected = shown.value?.building
            plan.select(null)
            assertEquals(listOf("A", null, "A"), listOf(initial, selected, shown.value?.building), "Wybrany podstawowy")
        }

    @Test
    fun switchesPlanOnSelectedCard() =
        runTest(UnconfinedTestDispatcher()) {
            val plan = MapPlan(Plans(setOf("A", "B")), entrances())
            val shown = collected(plan, CLOSE_CATALOG)
            plan.moveTo(INSIDE_A)
            plan.select("B")
            val switched = shown.value?.building
            plan.moveTo(FAR_WEST)
            val hiddenFar = shown.value?.building
            plan.moveTo(INSIDE_A)
            plan.select(null)
            assertEquals(listOf("B", null, "A"), listOf(switched, hiddenFar, shown.value?.building), "Wybrana karta")
        }

    @Test
    fun keepsCameraModeForCommands() =
        runTest(UnconfinedTestDispatcher()) {
            val plan = MapPlan(Plans(setOf("A", "B")), entrances())
            val shown = collected(plan, CLOSE_CATALOG)
            plan.show(MapFocus.Building(symbol = "B-8", command = 1L))
            plan.moveTo(INSIDE_A)
            val onShowBasic = shown.value?.building
            plan.show(MapFocus.Building(symbol = "A", command = 2L, floor = "P1"))
            plan.moveTo(INSIDE_B)
            assertEquals(listOf("A", "B"), listOf(onShowBasic, shown.value?.building), "Tryb środka mapy po poleceniu")
        }

    private fun MapPlan.moveTo(point: GeoPoint) = camera(MapCamera(center = point, zoom = 18.0))

    private fun entrances(floor: String? = null): ServerEntrances =
        ServerEntrances { symbol ->
            listOf(
                Entrance(
                    id = "w1",
                    buildingSymbol = symbol,
                    name = null,
                    isMain = true,
                    type = null,
                    location = INSIDE_A,
                    marking = EntranceMarking.ACCESSIBLE,
                    floorId = floor,
                ),
            )
        }

    private fun twoEntrances(): ServerEntrances =
        ServerEntrances { symbol ->
            listOf(
                entrance(id = "w1", symbol = symbol, isMain = true, floor = "P-1"),
                entrance(id = "w2", symbol = symbol, isMain = false, floor = "P1"),
            )
        }

    private fun entrance(id: String, symbol: String, isMain: Boolean, floor: String): Entrance =
        Entrance(
            id = id,
            buildingSymbol = symbol,
            name = null,
            isMain = isMain,
            type = null,
            location = INSIDE_A,
            marking = EntranceMarking.ACCESSIBLE,
            floorId = floor,
        )

    private fun synced(barriers: List<AcceptedBarrier>): AcceptedBarriers =
        object : AcceptedBarriers {
            override suspend fun barriers(): List<AcceptedBarrier> = barriers

            override fun syncedAt(): Instant? = Instant.parse("2026-10-14T10:00:00Z")
        }

    private fun barrierAt(entrance: String, severity: Severity) =
        AcceptedBarrier(
            id = "$entrance-${severity.value}",
            location = INSIDE_A,
            severity = severity,
            type = null,
            buildingSymbol = "A",
            verifiedAt = null,
            entranceId = entrance,
        )

    private fun TestScope.collected(plan: MapPlan, catalog: BuildingCatalog = CATALOG): MutableStateFlow<ShownPlan?> {
        val shown = MutableStateFlow<ShownPlan?>(null)
        backgroundScope.launch { plan.shown(MutableStateFlow(catalog)).collect { shown.value = it } }
        return shown
    }

    /** Plany z kopią budynków [stored]; zapisuje symbole pobrań. */
    private class Plans(private val stored: Set<String> = setOf("A")) : FloorPlans {
        val downloads = mutableListOf<String>()

        override suspend fun download(symbol: String): Boolean {
            downloads += symbol
            return true
        }

        override suspend fun floors(symbol: String): FloorList? =
            FloorList(source = SOURCE, floors = FLOORS).takeIf { symbol in stored }

        override suspend fun rooms(symbol: String, floor: String): RoomList? =
            RoomList(source = SOURCE, floorOutline = GeoMultiPolygon(emptyList()), rooms = emptyList())
                .takeIf { symbol in stored }
    }

    private companion object {
        const val SOURCE = "Politechnika Wrocławska"
        val INSIDE_A = GeoPoint(longitude = 17.00025, latitude = 51.00015)
        val INSIDE_B = GeoPoint(longitude = 17.00085, latitude = 51.00015)
        val FAR_WEST = GeoPoint(longitude = 16.998, latitude = 51.00015)
        val FLOORS =
            listOf(
                Floor(id = "P-1", label = "Piwnica", order = -1),
                Floor(id = "P0", label = "Parter", order = 0),
                Floor(id = "P1", label = "1 piętro", order = 1),
            )
        val CATALOG =
            BuildingCatalog(
                buildings = listOf(square("A", west = 17.0)),
                entrances = emptyList(),
                attribution = SOURCE,
                detailed = setOf("A"),
            )
        val CLOSE_CATALOG =
            CATALOG.copy(
                buildings = listOf(square("A", 17.0), square("B", 17.0006), square("B-8", 17.0012)),
                detailed = setOf("A", "B"),
            )

        fun square(symbol: String, west: Double): Building {
            val east = west + 0.0005
            val ring = listOf(west to 51.0, east to 51.0, east to 51.0003, west to 51.0003, west to 51.0)
            val outline = GeoPolygon(listOf(ring.map { (lon, lat) -> GeoPoint(longitude = lon, latitude = lat) }))
            return Building(symbol = symbol, name = null, outline = outline)
        }
    }
}
