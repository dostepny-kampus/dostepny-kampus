/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.report.removed

import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.building.NearestEntrance
import io.github.dostepnykampus.core.data.submission.sync.PendingPhotoDraft
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.submission.BarrierType
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionFloor
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import timber.log.Timber
import java.io.IOException
import kotlin.coroutines.EmptyCoroutineContext

/** Ekran C7: zdjęcie obowiązkowe, uciążliwości nie ma, zapis jak w C2. Wymagania: US8.4, BR-19, BR-7, NFR-7 */
@OptIn(ExperimentalCoroutinesApi::class)
class RemovedViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val saved = mutableListOf<Pair<SubmissionContent, List<PendingPhotoDraft>>>()
    private var failure: Exception? = null
    private val photo = PendingPhotoDraft(photoId = "p2", path = "/data/p2.jpg")
    private val lift =
        AcceptedBarrier(
            id = "tb9",
            location = GeoPoint(longitude = 17.06209, latitude = 51.10785),
            severity = Severity.BLOCKING,
            type = BarrierType.BROKEN_LIFT,
            buildingSymbol = "C-13",
            verifiedAt = null,
            floor = SubmissionFloor(number = 2, isMezzanine = false),
        )
    private val viewModel = viewModel(PLAIN)

    private fun viewModel(file: String, barrier: AcceptedBarrier = lift): RemovedViewModel {
        val catalog = BuildingCatalogRepository(EmptyCoroutineContext) { file }
        return RemovedViewModel(
            barriers = { listOf(barrier) },
            catalog = catalog,
            nearest = NearestEntrance(catalog),
            saver = { content, photos, _ ->
                failure?.let { throw it }
                saved.add(content to photos)
            },
            edits = { null },
        )
    }

    @BeforeEach
    fun main() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun reset() {
        Dispatchers.resetMain()
    }

    private fun open() {
        viewModel.open("tb9")
        dispatcher.scheduler.runCurrent()
    }

    /**
     * Nazwy budynku i wejścia z katalogu trafiają do stanu ekranu; zgłoszenie bez wejścia nie ma jego nazwy.
     *
     * Wymagania: US8.4, US1.5
     */
    @Test
    fun carriesNamesFromCatalog() =
        runTest(dispatcher) {
            val without = viewModel(NAMED)
            without.open("tb9")
            val with = viewModel(file = NAMED, barrier = lift.copy(entranceId = "n/1"))
            with.open("tb9")
            runCurrent()
            assertEquals("Gmach C" to null, without.uiState.value.names(), "zgłoszenie bez wejścia")
            assertEquals("Gmach C" to "Wejście A", with.uiState.value.names(), "zgłoszenie z wejściem")
        }

    /** Najbliższe wejście budynku bariery z katalogu trafia do stanu ekranu. Wymagania: US8.4, NFR-19 */
    @Test
    fun findsNearestEntrance() =
        runTest(dispatcher) {
            val named = viewModel(NAMED)
            named.open("tb9")
            runCurrent()
            val nearest = named.uiState.value.nearest
            assertEquals("n/1", nearest?.run { entrance.id }, "najbliższe wejście budynku C-13")
            assertEquals(null, viewModel.uiState.value.nearest, "bez wejść w katalogu nie ma odległości")
        }

    /**
     * Wysłanie bez zdjęcia kończy się błędem walidacji i niczego nie zapisuje.
     * Scenariusz: zgloszenia-zbierajacego.feature: Usunięcia bariery nie da się zgłosić bez nowego zdjęcia
     *
     * Wymagania: US8.4, BR-19
     */
    @Test
    fun blocksSendWithoutPhoto() =
        runTest(dispatcher) {
            open()
            viewModel.send()
            runCurrent()
            val expected = RemovedUiState(previous = lift, isPhotoMissing = true, stage = RemovedStage.EDITING)
            assertEquals(expected, viewModel.uiState.value, "brak zdjęcia zatrzymuje wysłanie")
            assertEquals(emptyList<Any>(), saved, "bez zdjęcia nic nie zapisano")
        }

    /**
     * Zgłoszenie ze zdjęciem zapisuje się z typem, miejscem i piętrem poprzedniego, bez uciążliwości.
     * Scenariusz: zgloszenia-zbierajacego.feature: Zgłoszenie „bariera usunięta” zapisuje się bez uciążliwości
     *
     * Wymagania: US8.4, BR-19, BR-7
     */
    @Test
    fun savesWithoutSeverity() =
        runTest(dispatcher) {
            open()
            viewModel.takePhoto(photo)
            viewModel.send()
            runCurrent()
            val expected =
                SubmissionContent(
                    kind = SubmissionKind.PRECISE,
                    location = lift.location,
                    photoIds = listOf("p2"),
                    buildingSymbol = "C-13",
                    floor = lift.floor,
                    barrierGroup = BarrierType.BROKEN_LIFT.group,
                    barrierType = "TB-9",
                    replacesSubmissionId = "tb9",
                    isBarrierRemoved = true,
                )
            assertEquals(listOf(expected to listOf(photo)), saved, "zapisana zmiana ze zdjęciem")
            assertEquals(RemovedStage.SAVED, viewModel.uiState.value.stage, "etap po zapisie")
        }

    /** Brak miejsca w telefonie zostawia zdjęcie i pozwala wysłać ponownie. Wymagania: NFR-7 */
    @Test
    fun retriesAfterStorageFailure() =
        runTest(dispatcher) {
            open()
            viewModel.takePhoto(photo)
            failure = IOException("brak miejsca")
            viewModel.send()
            runCurrent()

            assertEquals(RemovedStage.SAVE_FAILED, viewModel.uiState.value.stage, "etap po błędzie zapisu")
            assertEquals(emptyList<Any>(), saved, "nic nie zapisano po błędzie")
            failure = null
            viewModel.send()
            runCurrent()

            assertEquals(RemovedStage.SAVED, viewModel.uiState.value.stage, "etap po ponownym zapisie")
        }

    /** Błąd zapisu zostawia jedną linię z klasą wyjątku, bez jego komunikatu. Wymagania: NFR-57 */
    @Test
    fun logsStorageFailureClass() =
        runTest(dispatcher) {
            val lines = mutableListOf<String>()
            open()
            val tree =
                object : Timber.Tree() {
                    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                        lines += message
                    }
                }
            Timber.plant(tree)
            try {
                viewModel.takePhoto(photo)
                failure = IOException("brak miejsca")
                viewModel.send()
                runCurrent()
            } finally {
                Timber.uproot(tree)
            }

            assertEquals(
                listOf("Zapis zgłoszenia usuniętej bariery nieudany: IOException"),
                lines,
                "linia logu z klasą wyjątku bez komunikatu",
            )
        }

    private fun RemovedUiState.names() = buildingName to entranceName

    private companion object {
        const val PLAIN =
            """{"type":"FeatureCollection","attribution":"© plik","features":[""" +
                """{"type":"Feature","properties":{"kind":"building","symbols":["C-13"],"osm_id":"way/1"},""" +
                """"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}}]}"""
        const val NAMED =
            """{"type":"FeatureCollection","attribution":"© plik","features":[""" +
                """{"type":"Feature","properties":{"kind":"building","symbols":["C-13"],"display_name":"Gmach C",""" +
                """"osm_id":"way/1"},"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}},""" +
                """{"type":"Feature","properties":{"kind":"entrance","symbols":["C-13"],"osm_id":"n/1",""" +
                """"ref":"Wejście A"},"geometry":{"type":"Point","coordinates":[0,0]}}]}"""
    }
}
