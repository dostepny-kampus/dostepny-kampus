/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.report.marking

import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.submission.pending.PendingEdit
import io.github.dostepnykampus.core.data.submission.pending.PendingEdits
import io.github.dostepnykampus.core.data.submission.sync.EditedSubmission
import io.github.dostepnykampus.core.data.submission.sync.PendingPhotoDraft
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import io.github.dostepnykampus.feature.capture.navigation.MarkingRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException
import kotlin.coroutines.EmptyCoroutineContext

/** Ekran C6: wybór i zdjęcie obowiązkowe, zapis jak w C2. Wymagania: US17.2, BR-24, BR-7, NFR-7 */
@OptIn(ExperimentalCoroutinesApi::class)
class MarkingViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val saved = mutableListOf<Pair<SubmissionContent, List<PendingPhotoDraft>>>()
    private val edited = mutableListOf<EditedSubmission?>()
    private var failure: Exception? = null
    private var queued: PendingEdit? = null
    private val photo = PendingPhotoDraft(photoId = "p1", path = "/data/p1.jpg")
    private val viewModel =
        MarkingViewModel(
            catalog = BuildingCatalogRepository(EmptyCoroutineContext) { CATALOG },
            saver = { content, photos, edit ->
                failure?.let { throw it }
                saved.add(content to photos)
                edited.add(edit)
            },
            edits = PendingEdits { id -> queued?.takeIf { it.edited.id == id } },
        )

    @BeforeEach
    fun main() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun reset() {
        Dispatchers.resetMain()
    }

    private fun open(route: MarkingRoute = SIDE) {
        viewModel.open(route)
        dispatcher.scheduler.runCurrent()
    }

    private fun chooseWithPhoto(marking: EntranceMarking) {
        open()
        viewModel.choose(marking)
        viewModel.takePhoto(photo)
    }

    /** Wejście z obecnym oznaczeniem i nazwa budynku przychodzą z katalogu. Wymagania: US17.2, US17.3 */
    @Test
    fun opensEntranceFromCatalog() =
        runTest(dispatcher) {
            open()
            val state = viewModel.uiState.value
            assertEquals("n/1", state.entrance?.id, "wejście z trasy")
            assertEquals(EntranceMarking.NO_DATA, state.entrance?.marking, "obecne oznaczenie z katalogu")
            assertEquals("Gmach B", state.buildingName, "nazwa budynku z katalogu")
            assertEquals(MarkingStage.EDITING, state.stage, "ekran gotowy do wyboru")
        }

    /**
     * „Otwórz” na C3 przy własnej zmianie z kolejki: wybór i zdjęcie wracają, a zapis poprawia to samo zgłoszenie.
     *
     * Wymagania: US8.3, US17.2
     */
    @Test
    fun editsOwnChange() =
        runTest(dispatcher) {
            val own = PendingPhotoDraft(photoId = "p9", path = "/data/p9.jpg")
            val content =
                SubmissionContent(
                    kind = SubmissionKind.ENTRANCE_MARKING,
                    location = GeoPoint(longitude = 17.06, latitude = 51.1),
                    photoIds = listOf("p9"),
                    buildingSymbol = "B-8",
                    entranceId = "n/1",
                    entranceMarking = EntranceMarking.INACCESSIBLE,
                )
            queued =
                PendingEdit(edited = EditedSubmission(id = OWN, baseVersion = 3), content = content, photos = listOf(own))
            open(SIDE.copy(submissionId = OWN))
            val state = viewModel.uiState.value
            assertEquals(EntranceMarking.INACCESSIBLE, state.chosen, "zapisany wybór")
            assertEquals(own, state.photo, "zapisane zdjęcie")
            viewModel.send()
            runCurrent()
            assertEquals(listOf(EditedSubmission(id = OWN, baseVersion = 3)), edited, "zapis jako poprawka")
        }

    /** Zmiany nie ma już w telefonie, np. wysłała się w międzyczasie, więc ekran wraca do C3. Wymagania: US8.3 */
    @Test
    fun leavesWhenChangeGone() =
        runTest(dispatcher) {
            open(SIDE.copy(submissionId = OWN))
            assertEquals(MarkingStage.MISSING, viewModel.uiState.value.stage, "bez zmiany w kolejce")
        }

    /** Wejścia spoza budynku z trasy nie da się zmienić, więc ekran wraca. Wymagania: US17.2 */
    @Test
    fun leavesWhenEntranceMissing() =
        runTest(dispatcher) {
            open(SIDE.copy(buildingSymbol = "A-1"))
            assertEquals(MarkingStage.MISSING, viewModel.uiState.value.stage, "wejście nie należy do budynku")
        }

    /**
     * Wysłanie z wyborem, a bez zdjęcia, wskazuje brak zdjęcia i niczego nie zapisuje.
     * Scenariusz (częściowo): oznaczenie-wejscia.feature:
     *   Zmiany oznaczenia wejścia nie da się zapisać bez zdjęcia
     *
     * Wymagania: US17.2, BR-7, BR-24
     */
    @Test
    fun blocksSendWithoutPhoto() =
        runTest(dispatcher) {
            open()
            viewModel.choose(EntranceMarking.ACCESSIBLE)
            viewModel.send()
            runCurrent()
            val state = viewModel.uiState.value
            assertTrue(state.isPhotoMissing, "brak zdjęcia wskazany")
            assertFalse(state.isChoiceMissing, "wybór jest, więc bez jego błędu")
            assertEquals(emptyList<Any>(), saved, "bez zdjęcia nic nie zapisano")
        }

    /** Wysłanie ze zdjęciem, a bez wyboru, wskazuje brak wyboru i niczego nie zapisuje. Wymagania: BR-24 */
    @Test
    fun blocksSendWithoutChoice() =
        runTest(dispatcher) {
            open()
            viewModel.takePhoto(photo)
            viewModel.send()
            runCurrent()
            val state = viewModel.uiState.value
            assertTrue(state.isChoiceMissing, "brak wyboru wskazany")
            assertFalse(state.isPhotoMissing, "zdjęcie jest, więc bez jego błędu")
            assertEquals(emptyList<Any>(), saved, "bez wyboru nic nie zapisano")
        }

    /** „Brak danych” nadaje tylko import, więc nie jest do wyboru. Wymagania: BR-24 */
    @Test
    fun ignoresNoDataChoice() =
        runTest(dispatcher) {
            open()
            viewModel.choose(EntranceMarking.NO_DATA)
            assertNull(viewModel.uiState.value.chosen, "„brak danych” nie zostaje wybrane")
        }

    /**
     * Zmiana ze zdjęciem zapisuje się w telefonie jako zmiana oznaczenia wejścia, bez typu i uciążliwości.
     * Scenariusz (częściowo): oznaczenie-wejscia.feature:
     *   Zmiana oznaczenia wejścia przechodzi te same statusy co zgłoszenie
     *
     * Wymagania: US17.2, BR-24, BR-9
     */
    @Test
    fun savesMarkingChange() =
        runTest(dispatcher) {
            chooseWithPhoto(EntranceMarking.INACCESSIBLE)
            viewModel.send()
            runCurrent()
            val expected =
                SubmissionContent(
                    kind = SubmissionKind.ENTRANCE_MARKING,
                    location = GeoPoint(longitude = 17.06, latitude = 51.1),
                    photoIds = listOf("p1"),
                    buildingSymbol = "B-8",
                    entranceId = "n/1",
                    entranceMarking = EntranceMarking.INACCESSIBLE,
                )
            assertEquals(listOf(expected to listOf(photo)), saved, "zapisana zmiana ze zdjęciem")
            assertEquals(MarkingStage.SAVED, viewModel.uiState.value.stage, "etap po zapisie")
        }

    /** Brak miejsca w telefonie zostawia wybór i zdjęcie i pozwala wysłać ponownie. Wymagania: NFR-7 */
    @Test
    fun retriesAfterStorageFailure() =
        runTest(dispatcher) {
            chooseWithPhoto(EntranceMarking.ACCESSIBLE)
            failure = IOException("brak miejsca")
            viewModel.send()
            runCurrent()
            assertEquals(MarkingStage.SAVE_FAILED, viewModel.uiState.value.stage, "etap po błędzie zapisu")
            assertEquals(emptyList<Any>(), saved, "nic nie zapisano po błędzie")
            failure = null
            viewModel.send()
            runCurrent()
            assertEquals(MarkingStage.SAVED, viewModel.uiState.value.stage, "etap po ponownym zapisie")
        }

    private companion object {
        val SIDE = MarkingRoute(entranceId = "n/1", buildingSymbol = "B-8")
        const val OWN = "s-zmiana"
        const val CATALOG =
            """{"type":"FeatureCollection","attribution":"© plik","features":[""" +
                """{"type":"Feature","properties":{"kind":"building","symbols":["B-8"],"display_name":"Gmach B",""" +
                """"osm_id":"way/1"},"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}},""" +
                """{"type":"Feature","properties":{"kind":"entrance","symbols":["B-8"],"osm_id":"n/1"},""" +
                """"geometry":{"type":"Point","coordinates":[17.06,51.1]}}]}"""
    }
}
