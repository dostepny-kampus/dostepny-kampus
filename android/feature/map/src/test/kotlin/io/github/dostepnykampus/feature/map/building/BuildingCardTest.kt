/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
import io.github.dostepnykampus.core.data.barrier.AcceptedBarriers
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.network.NetworkStatus
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.model.building.Building
import io.github.dostepnykampus.core.model.building.BuildingCatalog
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.core.model.submission.BarrierType
import io.github.dostepnykampus.core.model.submission.Severity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.EmptyCoroutineContext

/** Wejście zalecane i kolejność listy oraz stan karty z katalogu. Wymagania: US2, US3, BR-25 */
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
    fun recommendsMainEntrance() {
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
    fun recommendsFirstWhenMainIsNot() {
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
    fun recommendsNamedWithoutMain() {
        val ordered =
            orderedEntrances(
                listOf(
                    entrance(id = "pwr", marking = EntranceMarking.ACCESSIBLE)
                        .copy(name = "Wejście", isNameGenerated = true),
                    entrance(id = "osm", marking = EntranceMarking.ACCESSIBLE).copy(name = null),
                    entrance(id = "dostosowane", marking = EntranceMarking.ACCESSIBLE),
                    entrance(id = "boczne", marking = EntranceMarking.ACCESSIBLE),
                ),
            )
        assertEquals(listOf("dostosowane", "boczne", "pwr", "osm"), ordered.map { it.entrance.id }, "kolejność")
        assertEquals("dostosowane", ordered.single { it.isRecommended }.entrance.id, "zalecane z nazwą")
    }

    @Test
    fun recommendsUnnamedMainOverNamed() {
        val ordered =
            orderedEntrances(
                listOf(
                    entrance(id = "dostosowane", marking = EntranceMarking.ACCESSIBLE),
                    entrance(id = "glowne", marking = EntranceMarking.ACCESSIBLE, isMain = true).copy(name = null),
                ),
            )
        assertEquals("glowne", ordered.single { it.isRecommended }.entrance.id, "główne bez nazwy wygrywa")
    }

    @Test
    fun recommendsFirstWhenNoneNamed() {
        val ordered =
            orderedEntrances(
                listOf(
                    entrance(id = "osm", marking = EntranceMarking.ACCESSIBLE).copy(name = " "),
                    entrance(id = "pwr", marking = EntranceMarking.ACCESSIBLE)
                        .copy(name = "Wejście", isNameGenerated = true),
                ),
            )
        assertEquals("osm", ordered.single { it.isRecommended }.entrance.id, "pierwsze bez nazw")
    }

    @Test
    fun putsInaccessibleFirst() {
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
    fun hidesServiceEntrances() {
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
    fun namesRecommendedInAnswer() {
        val ordered =
            orderedEntrances(
                listOf(
                    entrance(id = "boczne", marking = EntranceMarking.ACCESSIBLE),
                    entrance(id = "glowne", marking = EntranceMarking.ACCESSIBLE, isMain = true),
                ),
            )
        assertEquals(VisitAnswer.Accessible(id = "glowne", name = "glowne"), visitAnswer(ordered))
    }

    @Test
    fun answersWhenNone() {
        val stairs = orderedEntrances(listOf(entrance(id = "schody", marking = EntranceMarking.INACCESSIBLE)))
        assertEquals(VisitAnswer.NoAccessible(total = 1), visitAnswer(stairs))
        assertEquals(VisitAnswer.NoData, visitAnswer(emptyList()))
    }

    /** Istniejący budynek bez wejść odwiedzających ma osobną odpowiedź o braku danych. Wymagania: US3.1, US17.1 */
    @Test
    fun treatsServiceOnlyAsNoData() {
        val service =
            listOf(
                entrance(id = "awaryjne", marking = EntranceMarking.ACCESSIBLE, type = "emergency"),
                entrance(id = "wyjscie", marking = EntranceMarking.INACCESSIBLE, type = "exit"),
                entrance(id = "garaz", marking = EntranceMarking.NO_DATA, type = "garage"),
            )
        val catalog =
            BuildingCatalog(
                buildings = listOf(Building(symbol = "A-1", name = null, outline = GeoPolygon(emptyList()))),
                entrances = service,
                attribution = "©",
            )
        val card = buildingCard(catalog, "A-1") as BuildingCardUiState.Ready
        assertEquals(emptyList<OrderedEntrance>(), card.entrances)
        assertEquals(VisitAnswer.NoData, visitAnswer(card.entrances))
    }

    @Test
    fun countsMissingInAnswer() {
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
    fun handlesUnnamedAndService() {
        val unnamed = entrance(id = "boczne", marking = EntranceMarking.ACCESSIBLE).copy(name = null)
        assertEquals(VisitAnswer.Accessible(id = "boczne", name = null), visitAnswer(orderedEntrances(listOf(unnamed))))
        val service =
            orderedEntrances(
                listOf(
                    entrance(id = "awaryjne", marking = EntranceMarking.ACCESSIBLE, type = "emergency"),
                    entrance(id = "brak", marking = EntranceMarking.NO_DATA),
                ),
            )
        assertEquals(VisitAnswer.Unknown(missing = 1, total = 1), visitAnswer(service))
    }

    /** Karta konturu wspólnego bez planów łączy symbole i wejścia, a z planami zostaje osobna. Wymagania: US2, US3.2 */
    @Test
    fun buildsSameCardForSharedOutline() {
        val shared = GeoPolygon(listOf(listOf(GeoPoint(longitude = 0.0, latitude = 0.0))))
        val other = GeoPolygon(listOf(listOf(GeoPoint(longitude = 1.0, latitude = 1.0))))
        val t18Entrance = entrance(id = "e1", marking = EntranceMarking.ACCESSIBLE).copy(buildingSymbol = "T-18")
        val catalog =
            BuildingCatalog(
                buildings =
                    listOf("T-18", "T-22").map { Building(it, null, shared) } +
                        listOf("C-3", "C-4").map { Building(it, null, other) },
                entrances = listOf(t18Entrance, t18Entrance.copy(buildingSymbol = "T-22")),
                attribution = "©",
                detailed = setOf("C-3", "C-4"),
                hasServerList = true,
            )
        val first = buildingCard(catalog, "T-18") as BuildingCardUiState.Ready
        assertEquals(first, buildingCard(catalog, "T-22"), "oba symbole dają tę samą kartę")
        assertEquals(listOf("T-18", "T-22"), first.symbols, "symbole w nagłówku konturu wspólnego")
        assertEquals(1, first.entrances.size, "wspólne wejście bez powtórzenia")
        val detailed = buildingCard(catalog, "C-3") as BuildingCardUiState.Ready
        assertEquals(listOf("C-3"), detailed.symbols, "kontur z planami zostawia jeden symbol")
    }

    /** Model widoku karty konturu wspólnego daje ten sam stan i bariery dla obu symboli. Wymagania: US2, BR-14 */
    @Test
    fun mergesSharedBarriersInViewModel() =
        runTest(UnconfinedTestDispatcher()) {
            val geo =
                """{"type":"FeatureCollection","attribution":"©","features":[{"type":"Feature",""" +
                    """"properties":{"kind":"building","symbols":["T-18","T-22"],"osm_id":"way/1"},""" +
                    """"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}}]}"""
            val repo = BuildingCatalogRepository(EmptyCoroutineContext) { geo }
            val barrier =
                AcceptedBarrier(
                    id = "b1",
                    location = GeoPoint(longitude = 0.5, latitude = 0.5),
                    severity = Severity.HINDRANCE,
                    type = BarrierType.THRESHOLD,
                    buildingSymbol = "T-18",
                    verifiedAt = null,
                )
            val barriers =
                object : AcceptedBarriers {
                    override suspend fun barriers(): List<AcceptedBarrier> = listOf(barrier)

                    override fun syncedAt() = java.time.Instant.EPOCH
                }
            val first = BuildingCardViewModel("T-18", repo, online, acceptedBarriers = barriers)
            val second = BuildingCardViewModel("T-22", repo, online, acceptedBarriers = barriers)
            backgroundScope.launch { first.uiState.collect {} }
            backgroundScope.launch { second.uiState.collect {} }
            val state = first.uiState.value as BuildingCardUiState.Ready
            assertEquals(state, second.uiState.value, "oba symbole otwierają ten sam stan karty z barierami")
            assertEquals(listOf("b1"), state.barriers?.map { it.id }, "bariera przypisana do T-18")
        }

    @Test
    fun buildsReadyAndMissing() {
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

    /** Budynek podstawowy zna tylko lista serwera; z samego pliku konturów plan jest nieznany. Wymagania: US16.2 */
    @Test
    fun marksBasicOnlyFromServerList() {
        val outline = GeoPolygon(emptyList())
        val file =
            BuildingCatalog(
                buildings = listOf("A-1", "A-4").map { Building(symbol = it, name = null, outline = outline) },
                entrances = emptyList(),
                attribution = "©",
            )
        val server = file.copy(detailed = setOf("A-1"), hasServerList = true)
        assertEquals(false, isBasic(file, "A-4"), "katalog z pliku: plan nieznany, nie budynek podstawowy")
        assertEquals(true, isBasic(server, "A-4"), "lista serwera bez A-4 wśród szczegółowych: budynek podstawowy")
        assertEquals(false, isBasic(server, "A-1"), "A-1 szczegółowy według listy serwera")
    }

    /**
     * Budynek podstawowy z listy serwera ma swoje wejścia i nie ma faktów o piętrach, więc karta nie pokazuje
     * planu. Scenariusz (częściowo): budynki-bez-szczegolow.feature: Karta budynku podstawowego zamiast planu piętra
     *   pokazuje napis
     *
     * Wymagania: US16.2
     */
    @Test
    fun basicCardHasEntrances() {
        val outline = GeoPolygon(emptyList())
        val location = GeoPoint(longitude = 0.0, latitude = 0.0)
        val entrances =
            listOf("w1", "w2").map { id ->
                Entrance(
                    id = id,
                    buildingSymbol = "B-8",
                    name = id,
                    isMain = false,
                    type = "yes",
                    location = location,
                    marking = EntranceMarking.ACCESSIBLE,
                )
            }
        val catalog =
            BuildingCatalog(
                buildings = listOf("A-1", "B-8").map { Building(symbol = it, name = null, outline = outline) },
                entrances = entrances,
                attribution = "©",
                detailed = setOf("A-1"),
                hasServerList = true,
            )
        val card = buildingCard(catalog, "B-8") as BuildingCardUiState.Ready
        assertTrue(card.isBasic, "B-8 spoza listy szczegółowych jest budynkiem podstawowym")
        assertNull(card.floors, "budynek podstawowy nie ma faktów o piętrach")
        assertEquals(listOf("w1", "w2"), card.entrances.map { it.entrance.id }.sorted(), "wejścia budynku B-8")
    }

    @Test
    fun loadsCatalog() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = BuildingCardViewModel(symbol = "A-1", repository = repository, networkStatus = online)
            backgroundScope.launch { viewModel.uiState.collect {} }
            assertEquals(buildingCard(repository.catalog(), "A-1"), viewModel.uiState.value)
        }

    @Test
    fun retriesWhenMissing() =
        runTest(UnconfinedTestDispatcher()) {
            val viewModel = BuildingCardViewModel(symbol = "Z-9", repository = repository, networkStatus = online)
            backgroundScope.launch { viewModel.uiState.collect {} }
            assertEquals(BuildingCardUiState.Missing, viewModel.uiState.value)
            viewModel.retry()
            assertEquals(BuildingCardUiState.Missing, viewModel.uiState.value)
        }

    /**
     * Wiersz: V2, budynku nie ma w telefonie, „Spróbuj ponownie” → katalog czytany od nowa i karta budynku;
     *   zwykłe otwarcie karty nie czyta katalogu drugi raz.
     *
     * Wymagania: NFR-10
     */
    @Test
    fun showsNewBuildingOnRetry() =
        runTest(UnconfinedTestDispatcher()) {
            val reads = AtomicInteger()
            val server =
                BuildingCatalogRepository(EmptyCoroutineContext) {
                    if (reads.incrementAndGet() == 1) ASSET_JSON else ASSET_WITH_NEW_JSON
                }
            val opened = BuildingCardViewModel(symbol = "A-1", repository = server, networkStatus = online)
            backgroundScope.launch { opened.uiState.collect {} }
            val viewModel = BuildingCardViewModel(symbol = "N-1", repository = server, networkStatus = online)
            backgroundScope.launch { viewModel.uiState.collect {} }
            assertEquals(BuildingCardUiState.Missing, viewModel.uiState.value)
            assertEquals(1, reads.get())
            viewModel.retry()
            assertEquals("N-1", (viewModel.uiState.value as? BuildingCardUiState.Ready)?.symbol)
            assertEquals(2, reads.get())
        }

    @Test
    fun startsLoading() =
        runTest(UnconfinedTestDispatcher()) {
            val stopped = StandardTestDispatcher(TestCoroutineScheduler())
            val viewModel =
                BuildingCardViewModel(
                    symbol = "A-1",
                    repository = BuildingCatalogRepository(stopped) { error("plik nie jest czytany") },
                    networkStatus = online,
                )
            backgroundScope.launch { viewModel.uiState.collect {} }
            assertEquals(BuildingCardUiState.Loading, viewModel.uiState.value)
        }

    private fun isBasic(catalog: BuildingCatalog, symbol: String): Boolean =
        (buildingCard(catalog, symbol) as BuildingCardUiState.Ready).isBasic

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
        val online = NetworkStatus { flowOf(true) }

        const val ASSET_JSON =
            """{"type":"FeatureCollection","attribution":"© OpenStreetMap contributors","features":[""" +
                """{"type":"Feature","properties":{"kind":"building","symbols":["A-1"],"display_name":"Gmach",""" +
                """"osm_id":"way/1"},"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}},""" +
                """{"type":"Feature","properties":{"kind":"entrance","symbols":["A-1"],"osm_id":"node/2",""" +
                """"entrance":"yes","wheelchair":"yes","ref":"Boczna"},""" +
                """"geometry":{"type":"Point","coordinates":[1,0]}},""" +
                """{"type":"Feature","properties":{"kind":"entrance","symbols":["A-1"],"osm_id":"node/1",""" +
                """"entrance":"main","wheelchair":"no","ref":"Główne"},""" +
                """"geometry":{"type":"Point","coordinates":[0,0]}}]}"""

        val ASSET_WITH_NEW_JSON =
            ASSET_JSON.removeSuffix("]}") +
                """,{"type":"Feature","properties":{"kind":"building","symbols":["N-1"],"osm_id":"way/3"},""" +
                """"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}}]}"""

        val repository = BuildingCatalogRepository(EmptyCoroutineContext) { ASSET_JSON }
    }
}
