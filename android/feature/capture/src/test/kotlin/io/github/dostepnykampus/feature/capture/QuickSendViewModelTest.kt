/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.submissions.PendingPhotoDraft
import io.github.dostepnykampus.core.location.PositionFix
import io.github.dostepnykampus.core.location.PositionSource
import io.github.dostepnykampus.core.model.BarrierGroup
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.core.model.SubmissionFloor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException
import kotlin.coroutines.EmptyCoroutineContext

/** Krok 3 V4: bieżąca pozycja, piętro tylko w konturze i jeden zapis w kolejce (US4.1, US4.7, BR-14, NFR-7). */
@OptIn(ExperimentalCoroutinesApi::class)
class QuickSendViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val fixes = MutableSharedFlow<PositionFix>()
    private val saved = mutableListOf<SubmissionContent>()
    private var failure: Exception? = null
    private val report =
        QuickReport(
            photo = PendingPhotoDraft(photoId = "zdjecie-1", path = "/zdjecia/zdjecie-1.jpg"),
            group = BarrierGroup.TOILETS,
            floorNumber = 1,
        )
    private val viewModel by lazy {
        QuickSendViewModel(
            positions = PositionSource { fixes },
            catalog = BuildingCatalogRepository(EmptyCoroutineContext) { CAMPUS },
            saver = { content, _, _ ->
                failure?.let { throw it }
                saved.add(content)
            },
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

    // „Wyślij” czeka na pozycję i nie przerywa zgłoszenia upływem czasu (NFR-23).
    @Test
    fun nfr23_send_waits_for_position() =
        runTest(dispatcher) {
            watch()
            viewModel.send(report)
            runCurrent()
            assertNull(viewModel.position.value)
            assertEquals(emptyList<SubmissionContent>(), saved)
        }

    @Test
    fun brg2_floor_inside_contour() =
        runTest(dispatcher) {
            watch()
            fix(INSIDE_A1)
            viewModel.send(report)
            runCurrent()
            assertEquals(QuickSendStage.SENT, viewModel.stage.value)
            assertEquals(listOf(SubmissionFloor(number = 1)), saved.map { it.floor })
        }

    @Test
    fun br14_no_floor_outside() =
        runTest(dispatcher) {
            watch()
            fix(OUTSIDE)
            viewModel.send(report)
            runCurrent()
            assertEquals(listOf(null), saved.map { it.floor })
        }

    // Drugie „Wyślij” nie wysyła zdjęcia drugi raz (US4.7).
    @Test
    fun us4_7_sent_once() =
        runTest(dispatcher) {
            watch()
            fix(INSIDE_A1)
            viewModel.send(report)
            viewModel.send(report)
            runCurrent()
            viewModel.send(report)
            runCurrent()
            assertEquals(1, saved.size)
        }

    // Pełna pamięć: „Spróbuj ponownie” zostaje (NFR-7).
    @Test
    fun nfr7_failed_save() =
        runTest(dispatcher) {
            failure = IOException("pełna pamięć")
            watch()
            fix(INSIDE_A1)
            viewModel.send(report)
            runCurrent()
            assertEquals(QuickSendStage.FAILED, viewModel.stage.value)
        }

    private fun TestScope.watch() = backgroundScope.launch { viewModel.position.collect {} }.also { runCurrent() }

    private fun TestScope.fix(point: GeoPoint) {
        backgroundScope.launch { fixes.emit(PositionFix(point, 6.0, 0L)) }
        runCurrent()
    }

    private companion object {
        val INSIDE_A1 = GeoPoint(longitude = 17.0605, latitude = 51.1105)
        val OUTSIDE = GeoPoint(longitude = 17.10, latitude = 51.20)
        const val CAMPUS =
            """{"type":"FeatureCollection","attribution":"© OpenStreetMap","features":[""" +
                """{"type":"Feature","properties":{"kind":"building","symbols":["A-1"],"name":"A-1",""" +
                """"osm_id":"way/A-1"},"geometry":{"type":"Polygon","coordinates":[[[17.06,51.11],""" +
                """[17.061,51.11],[17.061,51.111],[17.06,51.111],[17.06,51.11]]]}}]}"""
    }
}
