/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.report.removed

import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
import io.github.dostepnykampus.core.data.submission.sync.PendingPhotoDraft
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.core.model.SubmissionFloor
import io.github.dostepnykampus.core.model.SubmissionKind
import io.github.dostepnykampus.core.model.submission.BarrierType
import io.github.dostepnykampus.core.model.submission.Severity
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
import java.io.IOException

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
    private val viewModel =
        RemovedViewModel(
            barriers = { listOf(lift) },
            saver = { content, photos, _ ->
                failure?.let { throw it }
                saved.add(content to photos)
            },
        )

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
     * Wysłanie bez zdjęcia kończy się błędem walidacji i niczego nie zapisuje.
     * Scenariusz: zgloszenia-zbierajacego.feature: Usunięcia bariery nie da się zgłosić bez nowego zdjęcia
     *
     * Wymagania: US8.4, BR-19
     */
    @Test
    fun `us8_4 bez zdjęcia nie wychodzi`() =
        runTest(dispatcher) {
            open()
            viewModel.send()
            runCurrent()
            val expected = RemovedUiState(previous = lift, isPhotoMissing = true, stage = RemovedStage.EDITING)
            assertEquals(expected, viewModel.uiState.value)
            assertEquals(emptyList<Any>(), saved)
        }

    /**
     * Zgłoszenie ze zdjęciem zapisuje się z typem, miejscem i piętrem poprzedniego, bez uciążliwości.
     * Scenariusz: zgloszenia-zbierajacego.feature: Zgłoszenie „bariera usunięta” zapisuje się bez uciążliwości
     *
     * Wymagania: US8.4, BR-19, BR-7
     */
    @Test
    fun `us8_4 zapis bez uciążliwości`() =
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
            assertEquals(listOf(expected to listOf(photo)), saved)
            assertEquals(RemovedStage.SAVED, viewModel.uiState.value.stage)
        }

    /** Brak miejsca w telefonie zostawia zdjęcie i pozwala wysłać ponownie. Wymagania: NFR-7 */
    @Test
    fun `nfr7 brak miejsca i ponowienie`() =
        runTest(dispatcher) {
            open()
            viewModel.takePhoto(photo)
            failure = IOException("brak miejsca")
            viewModel.send()
            runCurrent()

            assertEquals(RemovedStage.SAVE_FAILED, viewModel.uiState.value.stage)
            assertEquals(emptyList<Any>(), saved)
            failure = null
            viewModel.send()
            runCurrent()

            assertEquals(RemovedStage.SAVED, viewModel.uiState.value.stage)
        }
}
