/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.BuildingCatalog
import io.github.dostepnykampus.core.model.Entrance
import io.github.dostepnykampus.core.model.EntranceMarking
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.coroutines.EmptyCoroutineContext

/** Wejście zalecane i kolejność listy (BR-25) oraz stan karty z katalogu. */
@OptIn(ExperimentalCoroutinesApi::class)
class BuildingCardTest {
    @BeforeEach
    fun main() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun reset() {
        Dispatchers.resetMain()
    }

    @Test
    fun br25_main_is_recommended() {
        val ordered =
            orderedEntrances(
                listOf(
                    entrance(id = "boczne", marking = EntranceMarking.ACCESSIBLE),
                    entrance(id = "glowne", marking = EntranceMarking.ACCESSIBLE, isMain = true),
                ),
            )
        assertEquals(listOf("glowne", "boczne"), ordered.map { it.entrance.id })
        assertEquals(listOf(true, false), ordered.map { it.isRecommended })
    }

    @Test
    fun br25_first_when_main_is_not() {
        val ordered =
            orderedEntrances(
                listOf(
                    entrance(id = "boczne", marking = EntranceMarking.ACCESSIBLE),
                    entrance(id = "dziedziniec", marking = EntranceMarking.ACCESSIBLE),
                    entrance(id = "glowne", marking = EntranceMarking.INACCESSIBLE, isMain = true),
                ),
            )
        assertEquals(listOf("boczne", "dziedziniec", "glowne"), ordered.map { it.entrance.id })
        assertEquals("boczne", ordered.single { it.isRecommended }.entrance.id)
    }

    @Test
    fun br25_inaccessible_come_first() {
        val ordered =
            orderedEntrances(
                listOf(
                    entrance(id = "brak", marking = EntranceMarking.NO_DATA, isMain = true),
                    entrance(id = "schody", marking = EntranceMarking.INACCESSIBLE),
                    entrance(id = "brama", marking = EntranceMarking.NO_DATA),
                    entrance(id = "tyl", marking = EntranceMarking.INACCESSIBLE),
                ),
            )
        assertEquals(listOf("schody", "tyl", "brak", "brama"), ordered.map { it.entrance.id })
        assertEquals(emptyList<String>(), ordered.filter { it.isRecommended }.map { it.entrance.id })
    }

    @Test
    fun br25_hides_service_entrances() {
        val ordered =
            orderedEntrances(
                listOf(
                    entrance(id = "awaryjne", marking = EntranceMarking.ACCESSIBLE, type = "emergency"),
                    entrance(id = "wyjscie", marking = EntranceMarking.ACCESSIBLE, type = "exit"),
                    entrance(id = "garaz", marking = EntranceMarking.ACCESSIBLE, type = "garage", isMain = true),
                    entrance(id = "boczne", marking = EntranceMarking.NO_DATA),
                ),
            )
        assertEquals(listOf("boczne"), ordered.map { it.entrance.id })
        assertEquals(false, ordered.single().isRecommended)
    }

    @Test
    fun us3_answer_names_recommended() {
        val ordered =
            orderedEntrances(
                listOf(
                    entrance(id = "boczne", marking = EntranceMarking.ACCESSIBLE),
                    entrance(id = "glowne", marking = EntranceMarking.ACCESSIBLE, isMain = true),
                ),
            )
        assertEquals(VisitAnswer.Accessible("glowne"), visitAnswer(ordered))
    }

    @Test
    fun us3_answer_when_none() {
        val stairs = orderedEntrances(listOf(entrance(id = "schody", marking = EntranceMarking.INACCESSIBLE)))
        assertEquals(VisitAnswer.NoAccessible, visitAnswer(stairs))
        assertEquals(VisitAnswer.NoAccessible, visitAnswer(emptyList()))
    }

    @Test
    fun us3_answer_counts_missing() {
        val ordered =
            orderedEntrances(
                listOf(
                    entrance(id = "schody", marking = EntranceMarking.INACCESSIBLE),
                    entrance(id = "brak", marking = EntranceMarking.NO_DATA),
                    entrance(id = "brama", marking = EntranceMarking.NO_DATA),
                ),
            )
        assertEquals(VisitAnswer.Unknown(missing = 2, total = 3), visitAnswer(ordered))
    }

    @Test
    fun us3_unnamed_and_service() {
        val unnamed = entrance(id = "boczne", marking = EntranceMarking.ACCESSIBLE).copy(name = null)
        assertEquals(VisitAnswer.Accessible(null), visitAnswer(orderedEntrances(listOf(unnamed))))
        val service =
            orderedEntrances(
                listOf(
                    entrance(id = "awaryjne", marking = EntranceMarking.ACCESSIBLE, type = "emergency"),
                    entrance(id = "brak", marking = EntranceMarking.NO_DATA),
                ),
            )
        assertEquals(VisitAnswer.Unknown(missing = 1, total = 1), visitAnswer(service))
    }

    @Test
    fun us2_ready_and_missing() {
        val catalog =
            BuildingCatalog(
                buildings = listOf(Building(symbol = "A-1", name = "Gmach", outline = GeoPolygon(emptyList()))),
                entrances = listOf(entrance(id = "boczne", marking = EntranceMarking.ACCESSIBLE)),
                attribution = "©",
            )
        val ready = buildingCard(catalog, "A-1") as BuildingCardUiState.Ready
        assertEquals("A-1", ready.symbol)
        assertEquals("Gmach", ready.name)
        val listed = ready.entrances.single()
        assertEquals("boczne", listed.entrance.id)
        assertEquals(BuildingCardUiState.Missing, buildingCard(catalog, "Z-9"))
    }

    @Test
    fun us2_view_model_loads_catalog() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = BuildingCardViewModel(symbol = "A-1", repository = repository)
            backgroundScope.launch { viewModel.uiState.collect {} }
            assertEquals(buildingCard(repository.catalog(), "A-1"), viewModel.uiState.value)
        }

    @Test
    fun us2_view_model_missing_retries() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = BuildingCardViewModel(symbol = "Z-9", repository = repository)
            backgroundScope.launch { viewModel.uiState.collect {} }
            assertEquals(BuildingCardUiState.Missing, viewModel.uiState.value)
            viewModel.retry()
            assertEquals(BuildingCardUiState.Missing, viewModel.uiState.value)
        }

    @Test
    fun us2_view_model_loading() =
        runTest(UnconfinedTestDispatcher()) {
            val stopped = StandardTestDispatcher(TestCoroutineScheduler())
            val viewModel =
                BuildingCardViewModel(
                    symbol = "A-1",
                    repository = BuildingCatalogRepository(stopped) { error("plik nie jest czytany") },
                )
            backgroundScope.launch { viewModel.uiState.collect {} }
            assertEquals(BuildingCardUiState.Loading, viewModel.uiState.value)
        }

    private fun entrance(id: String, marking: EntranceMarking, isMain: Boolean = false, type: String? = "yes") =
        Entrance(
            id = id,
            buildingSymbol = "A-1",
            name = id,
            isMain = isMain,
            type = type,
            location = GeoPoint(longitude = 0.0, latitude = 0.0),
            marking = marking,
        )

    private companion object {
        val repository =
            BuildingCatalogRepository(EmptyCoroutineContext) {
                """{"type":"FeatureCollection","attribution":"© OpenStreetMap contributors","features":[""" +
                    """{"type":"Feature","properties":{"kind":"building","symbols":["A-1"],"name":"Gmach",""" +
                    """"osm_id":"way/1"},"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}},""" +
                    """{"type":"Feature","properties":{"kind":"entrance","symbols":["A-1"],"osm_id":"node/2",""" +
                    """"entrance":"yes","wheelchair":"yes","ref":"Boczna"},""" +
                    """"geometry":{"type":"Point","coordinates":[1,0]}},""" +
                    """{"type":"Feature","properties":{"kind":"entrance","symbols":["A-1"],"osm_id":"node/1",""" +
                    """"entrance":"main","wheelchair":"no","ref":"Główne"},""" +
                    """"geometry":{"type":"Point","coordinates":[0,0]}}]}"""
            }
    }
}
