/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.plan

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
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

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
    fun startsOnGroundForUnknownFloor() =
        runTest(UnconfinedTestDispatcher()) {
            val plan = MapPlan(Plans(), entrances(floor = "P7"))
            val shown = collected(plan)
            plan.camera(MapCamera(center = INSIDE_A, zoom = 18.0))
            assertEquals("P0", shown.value?.floor, "Piętro wejścia spoza planu nie dało parteru")
        }

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

    private fun TestScope.collected(plan: MapPlan): MutableStateFlow<ShownPlan?> {
        val shown = MutableStateFlow<ShownPlan?>(null)
        backgroundScope.launch { plan.shown(MutableStateFlow(CATALOG)).collect { shown.value = it } }
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
        val FLOORS =
            listOf(
                Floor(id = "P-1", label = "Piwnica", order = -1),
                Floor(id = "P0", label = "Parter", order = 0),
                Floor(id = "P1", label = "1 piętro", order = 1),
            )
        val RING = listOf(17.0 to 51.0, 17.0005 to 51.0, 17.0005 to 51.0003, 17.0 to 51.0003, 17.0 to 51.0)
        val OUTLINE = GeoPolygon(listOf(RING.map { (lon, lat) -> GeoPoint(longitude = lon, latitude = lat) }))
        val CATALOG =
            BuildingCatalog(
                buildings = listOf(Building(symbol = "A", name = null, outline = OUTLINE)),
                entrances = emptyList(),
                attribution = SOURCE,
                detailed = setOf("A"),
            )
    }
}
