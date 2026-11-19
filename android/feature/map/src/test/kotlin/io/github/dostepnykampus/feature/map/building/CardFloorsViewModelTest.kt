/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import androidx.lifecycle.viewModelScope
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.room.FloorPlans
import io.github.dostepnykampus.core.model.GeoMultiPolygon
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.Floor
import io.github.dostepnykampus.core.model.building.FloorList
import io.github.dostepnykampus.core.model.room.Room
import io.github.dostepnykampus.core.model.room.RoomCategory
import io.github.dostepnykampus.core.model.room.RoomList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.coroutines.EmptyCoroutineContext

/** Fakty o piętrach na stanie karty V2 z planu budynku. Wymagania: US16.3 */
@OptIn(ExperimentalCoroutinesApi::class)
class CardFloorsViewModelTest {
    @Test
    fun readsLiftFromBuildingPlan() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val facts = cardFloors(isStored = true)
            Dispatchers.resetMain()
            assertEquals(
                listOf("Parter"),
                facts?.elevator?.run { floors.map { it.name } },
                "Karta nie wzięła windy z planu budynku",
            )
        }

    /** Wiersz: plan w telefonie, pobieranie nowego planu trwa → karta ma fakty z kopii, nie czeka na serwer. */
    @Test
    fun readsStoredPlanBeforeDownload() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val facts = cardFloors(isStored = true, isDownloading = true)
            Dispatchers.resetMain()
            assertEquals(
                listOf("Parter"),
                facts?.elevator?.run { floors.map { it.name } },
                "Karta czekała na pobranie planu, choć miała jego kopię",
            )
        }

    @Test
    fun hasNoFloorsWithoutPlan() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val facts = cardFloors(isStored = false)
            Dispatchers.resetMain()
            assertEquals(null, facts, "Budynek bez planu w telefonie dostał fakty o piętrach")
        }

    private fun TestScope.cardFloors(isStored: Boolean, isDownloading: Boolean = false): FloorFacts? {
        val card =
            BuildingCardViewModel(
                symbol = "A-1",
                repository = BuildingCatalogRepository(EmptyCoroutineContext) { ASSET_JSON },
                networkStatus = { flowOf(true) },
                plans = Plans(isStored, isDownloading),
            )
        val collector = backgroundScope.launch { card.uiState.collect {} }
        advanceUntilIdle()
        val ready = card.uiState.value as BuildingCardUiState.Ready
        collector.cancel()
        card.viewModelScope.cancel()
        advanceUntilIdle()
        return ready.floors
    }

    /**
     * Plan budynku A-1 z windą na parterze, gdy [isStored]; bez kopii nic nie ma. Pobieranie trwa bez końca, gdy
     * [isDownloading].
     */
    private class Plans(private val isStored: Boolean, private val isDownloading: Boolean) : FloorPlans {
        override suspend fun download(symbol: String): Boolean = if (isDownloading) awaitCancellation() else isStored

        override suspend fun floors(symbol: String): FloorList? =
            FloorList(source = SOURCE, floors = listOf(Floor(id = "P0", label = "Parter", order = 0)))
                .takeIf { isStored }

        override suspend fun rooms(symbol: String, floor: String): RoomList? =
            RoomList(source = SOURCE, floorOutline = NO_OUTLINE, rooms = listOf(LIFT)).takeIf { isStored }
    }

    private companion object {
        const val SOURCE = "Politechnika Wrocławska"
        const val ASSET_JSON =
            """{"type":"FeatureCollection","attribution":"© OpenStreetMap contributors","features":[""" +
                """{"type":"Feature","properties":{"kind":"building","symbols":["A-1"],"display_name":"Gmach",""" +
                """"osm_id":"way/1"},"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}}]}"""
        val NO_OUTLINE = GeoMultiPolygon(emptyList())
        val LIFT =
            Room(
                id = "w",
                number = null,
                function = "Winda",
                category = RoomCategory.ELEVATOR,
                geometry = NO_OUTLINE,
                labelPoint = GeoPoint(longitude = 0.5, latitude = 0.5),
                isLabelled = false,
            )
    }
}
