/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.report.removed

import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.building.NearestEntrance
import io.github.dostepnykampus.core.data.submission.pending.PendingEdit
import io.github.dostepnykampus.core.data.submission.sync.EditedSubmission
import io.github.dostepnykampus.core.data.submission.sync.PendingPhotoDraft
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
import kotlin.coroutines.EmptyCoroutineContext

/** C7 z C3: poprawka własnego zgłoszenia „bariera usunięta” z kolejki telefonu. Wymagania: US8.3, US8.4, BR-19 */
@OptIn(ExperimentalCoroutinesApi::class)
class RemovedEditTest {
    private val dispatcher = StandardTestDispatcher()
    private val saved = mutableListOf<Triple<Int, List<PendingPhotoDraft>, EditedSubmission?>>()
    private val photo = PendingPhotoDraft(photoId = "p2", path = "/data/p2.jpg")
    private val lift = acceptedLift
    private val own =
        PendingEdit(
            edited = EditedSubmission(id = OWN, baseVersion = 3),
            content = lift.removalContent(photo),
            photos = listOf(photo),
        )
    private var queued: PendingEdit? = own
    private val viewModel by lazy {
        val catalog = BuildingCatalogRepository(EmptyCoroutineContext) { EMPTY }
        RemovedViewModel(
            barriers = { listOf(lift) },
            catalog = catalog,
            nearest = NearestEntrance(catalog),
            saver = { content, photos, edited -> saved.add(Triple(content.photoIds.size, photos, edited)) },
            edits = { id -> queued?.takeIf { it.edited.id == id } },
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

    /** Zgłoszenie z kolejki otwiera się z poprzednim zgłoszeniem i zapisanym zdjęciem. Wymagania: US8.3, US8.4 */
    @Test
    fun opensWithSavedPhoto() =
        runTest(dispatcher) {
            viewModel.openEdit(OWN)
            runCurrent()

            val state = viewModel.uiState.value
            assertEquals(lift, state.previous, "poprzednie zgłoszenie z powiązania")
            assertEquals(photo, state.photo, "zapisane zdjęcie")
            assertEquals(RemovedStage.EDITING, state.stage, "etap po wczytaniu")
        }

    /** Zapis idzie jako poprawka tego zgłoszenia, więc kolejka nie ma dwóch. Wymagania: US8.3, BR-9 */
    @Test
    fun savesAsEdit() =
        runTest(dispatcher) {
            val fresh = PendingPhotoDraft(photoId = "p3", path = "/data/p3.jpg")
            viewModel.openEdit(OWN)
            runCurrent()
            viewModel.takePhoto(fresh)
            viewModel.send()
            runCurrent()

            assertEquals(listOf(Triple(1, listOf(fresh), own.edited)), saved, "poprawka z nowym zdjęciem")
            assertEquals(RemovedStage.SAVED, viewModel.uiState.value.stage, "etap po zapisie")
        }

    /** Zgłoszenia, którego już nie ma w telefonie, ekran nie otwiera. Wymagania: US8.3 */
    @Test
    fun missingWithoutQueuedEdit() =
        runTest(dispatcher) {
            queued = null
            viewModel.openEdit(OWN)
            runCurrent()

            assertEquals(RemovedStage.MISSING, viewModel.uiState.value.stage, "zgłoszenie wysłało się w międzyczasie")
        }

    private companion object {
        const val OWN = "s-usuniecie"
        const val EMPTY = """{"type":"FeatureCollection","attribution":"© plik","features":[]}"""
    }
}
