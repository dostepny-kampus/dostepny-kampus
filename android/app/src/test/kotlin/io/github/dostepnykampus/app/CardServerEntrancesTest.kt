/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.buildings.ServerEntrances
import io.github.dostepnykampus.core.data.buildings.ServerEntrancesRepository
import io.github.dostepnykampus.core.model.AssessmentSource
import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.Entrance
import io.github.dostepnykampus.core.model.EntranceAssessment
import io.github.dostepnykampus.core.model.EntranceMarking
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.core.network.BuildingDetails
import io.github.dostepnykampus.core.network.DetailLevel
import io.github.dostepnykampus.feature.map.BuildingCardUiState
import io.github.dostepnykampus.feature.map.BuildingCardViewModel
import io.github.dostepnykampus.feature.map.OrderedEntrance
import io.github.dostepnykampus.feature.map.buildingCard
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.LocalDate
import kotlin.coroutines.EmptyCoroutineContext

/** Wejścia karty V2 od karty budynku z serwera do stanu karty (US17.3, NFR-10, NFR-12). */
@OptIn(ExperimentalCoroutinesApi::class)
class CardServerEntrancesTest {
    private val catalog = BuildingCatalogRepository(EmptyCoroutineContext) { ASSET_JSON }
    private val fileCard by lazy { runBlocking { buildingCard(catalog.catalog(), "A-1") } }

    @BeforeEach
    fun main() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun reset() {
        Dispatchers.resetMain()
    }

    /**
     * Wiersz: V2 budynku, który zna serwer → tekst karty od razu z telefonu, a gdy serwer odpowie, wejścia z serwera
     *   z dniem i źródłem zamiast wejść z pliku, US17.3, NFR-12.
     */
    @Test
    fun us17_3_server_after_phone() {
        val answer = CompletableDeferred<ApiResult<BuildingDetails>>()
        val card = openCard { answer.await() }
        assertEquals(fileCard, card.settled { it is BuildingCardUiState.Ready }, "karta czeka na serwer")
        answer.complete(ApiResult.Success(DETAILS))
        assertEquals(listOf(SERVER_ENTRANCE), card.datedEntrances())
    }

    /**
     * Wiersz: V2 bez sieci albo przy błędzie serwera → wejścia z pliku bez dnia i źródła, bez błędu i bez pustej
     *   listy, NFR-10.
     */
    @Test
    fun nfr10_failure_keeps_file() {
        val failures = listOf(ApiResult.NetworkError(IOException("brak sieci")), ApiResult.ServerError(SERVER_ERROR))
        failures.forEach { failure ->
            val asked = CompletableDeferred<Unit>()
            val card =
                openCard {
                    asked.complete(Unit)
                    failure
                }
            runBlocking { withTimeoutOrNull(WAIT_MS) { asked.await() } }
            assertEquals(fileCard, card.settled { it is BuildingCardUiState.Ready }, failure.toString())
        }
    }

    /** Wiersz: kolejne otwarcie V2 bez sieci → zostają wejścia pobrane wcześniej z serwera, NFR-10. */
    @Test
    fun nfr10_reopen_keeps_server() {
        val answers = ArrayDeque(listOf(ApiResult.Success(DETAILS), ApiResult.NetworkError(IOException("brak sieci"))))
        val source = ServerEntrancesRepository { answers.removeFirst() }
        cardOf(source).datedEntrances()
        assertEquals(listOf(SERVER_ENTRANCE), cardOf(source).datedEntrances())
    }

    /** Wiersz: zamknięcie karty w trakcie pobierania → wywołanie serwera przerwane, bez działającego zapytania. */
    @Test
    fun closing_card_cancels_call() {
        val started = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        val store = ViewModelStore()
        val factory =
            viewModelFactory {
                initializer {
                    openCard {
                        started.complete(Unit)
                        waitForever(cancelled)
                    }
                }
            }
        ViewModelProvider.create(store, factory)[BuildingCardViewModel::class].settled { false }
        runBlocking { withTimeoutOrNull(WAIT_MS) { started.await() } }
        store.clear()
        runBlocking { withTimeoutOrNull(WAIT_MS) { cancelled.await() } }
        assertTrue(started.isCompleted && cancelled.isCompleted, "pobranie nie zaczęło się albo trwa po zamknięciu")
    }

    private fun openCard(details: suspend (String) -> ApiResult<BuildingDetails>): BuildingCardViewModel =
        cardOf(ServerEntrancesRepository(details))

    private fun cardOf(source: ServerEntrances): BuildingCardViewModel =
        BuildingCardViewModel(
            symbol = "A-1",
            repository = catalog,
            networkStatus = { flowOf(true) },
            serverEntrances = source,
        )

    private suspend fun waitForever(cancelled: CompletableDeferred<Unit>): Nothing =
        try {
            awaitCancellation()
        } finally {
            cancelled.complete(Unit)
        }

    private fun BuildingCardViewModel.settled(until: (BuildingCardUiState) -> Boolean): BuildingCardUiState =
        runBlocking { withTimeoutOrNull(WAIT_MS) { uiState.first(until) } ?: uiState.value }

    private fun BuildingCardViewModel.datedEntrances(): List<Entrance> {
        val dated = settled { it is BuildingCardUiState.Ready && it.entrances.any(::isDated) }
        return if (dated is BuildingCardUiState.Ready) dated.entrances.map { it.entrance } else emptyList()
    }

    private fun isDated(listed: OrderedEntrance): Boolean = listed.entrance.assessment != null

    private companion object {
        const val WAIT_MS = 1_000L
        const val SERVER_ERROR = 500

        val SERVER_ENTRANCE =
            Entrance(
                id = "a1-main",
                buildingSymbol = "A-1",
                name = "Wejście główne",
                isMain = true,
                type = null,
                location = GeoPoint(longitude = 0.5, latitude = 0.0),
                marking = EntranceMarking.ACCESSIBLE,
                assessment = EntranceAssessment(LocalDate.of(2026, 10, 1), AssessmentSource.WALKTHROUGH),
            )

        val DETAILS =
            BuildingDetails(
                building = Building(symbol = "A-1", name = "Gmach", outline = GeoPolygon(emptyList())),
                detailLevel = DetailLevel.BASIC,
                entrances = listOf(SERVER_ENTRANCE),
            )

        const val ASSET_JSON =
            """{"type":"FeatureCollection","attribution":"© OpenStreetMap contributors","features":[""" +
                """{"type":"Feature","properties":{"kind":"building","symbols":["A-1"],"name":"Gmach",""" +
                """"osm_id":"way/1"},"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}},""" +
                """{"type":"Feature","properties":{"kind":"entrance","symbols":["A-1"],"osm_id":"node/1",""" +
                """"entrance":"yes","wheelchair":"yes","ref":"Boczne"},""" +
                """"geometry":{"type":"Point","coordinates":[1,0]}}]}"""
    }
}
