/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.measure

import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.submissions.PendingPhotoDraft
import io.github.dostepnykampus.core.location.ElapsedClock
import io.github.dostepnykampus.core.location.LocationSwitch
import io.github.dostepnykampus.core.location.PositionFix
import io.github.dostepnykampus.core.location.PositionSource
import io.github.dostepnykampus.core.location.SatelliteSource
import io.github.dostepnykampus.core.model.BarrierType
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.Severity
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.feature.capture.BuildingLookup
import io.github.dostepnykampus.feature.capture.quick.content
import io.github.dostepnykampus.feature.capture.report.ReportEdit
import io.github.dostepnykampus.feature.capture.report.ReportFloor
import io.github.dostepnykampus.feature.capture.report.ReportForm
import io.github.dostepnykampus.feature.capture.report.ReportFormViewModel
import io.github.dostepnykampus.feature.capture.report.ReportStage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Cała ścieżka od pomiaru C1 do zapisanego zgłoszenia: pomiar podpowiada budynek, Zbierający decyduje, „Dalej”
 * przekazuje miejsce do formularza C2, a „Zapisz” oddaje treść zgłoszenia.
 *
 * Wymagania: US6.2, BR-G2
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MeasuredReportTest {
    private val dispatcher = StandardTestDispatcher()
    private val fixes = MutableSharedFlow<PositionFix>(extraBufferCapacity = 1)
    private val saved = mutableListOf<SubmissionContent>()
    private val measure by lazy {
        MeasureViewModel(
            positions = PositionSource { fixes },
            satellites = SatelliteSource { emptyFlow() },
            clock = ElapsedClock { dispatcher.scheduler.currentTime },
            locationSwitch = LocationSwitch { MutableStateFlow(true) },
            buildingLookup = BuildingLookup(BuildingCatalogRepository(dispatcher) { CAMPUS }, dispatcher),
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

    /**
     * „Potwierdź” przy podpowiedzi A-1: zapisane zgłoszenie ma budynek A-1.
     * Scenariusz (przykład: potwierdzi budynek A-1): pomiar-precyzyjny.feature: Zbierający decyduje o podpowiedzianym
     *   budynku
     */
    @Test
    fun us6_2_confirmed_building_saved() =
        runTest(dispatcher) {
            suggestA1()
            measure.confirm()
            assertEquals("A-1", savedReport().buildingSymbol)
        }

    /**
     * „Wybierz inny” i B-1 z listy: zapisane zgłoszenie ma budynek B-1.
     * Scenariusz (przykład: wybierze budynek B-1): pomiar-precyzyjny.feature: Zbierający decyduje o podpowiedzianym
     *   budynku
     */
    @Test
    fun us6_2_other_building_saved() =
        runTest(dispatcher) {
            suggestA1()
            measure.choose("B-1")
            assertEquals("B-1", savedReport().buildingSymbol)
        }

    /**
     * „Poza budynkiem”: zapisane zgłoszenie nie ma budynku ani piętra.
     * Scenariusz (przykład: zostawi „poza budynkiem”): pomiar-precyzyjny.feature: Zbierający decyduje o podpowiedzianym
     *   budynku
     */
    @Test
    fun br_g2_outside_no_building() =
        runTest(dispatcher) {
            suggestA1()
            measure.outside()
            val report = savedReport()
            assertNull(report.buildingSymbol)
            assertNull(report.floor)
        }

    private fun TestScope.suggestA1() {
        backgroundScope.launch { measure.uiState.collect {} }
        runCurrent()
        backgroundScope.launch { fixes.emit(PositionFix(IN_A1, ACCURACY, dispatcher.scheduler.currentTime)) }
        runCurrent()
        advanceTimeBy(MEASUREMENT_MS)
        runCurrent()
        val place = (measure.uiState.value as MeasureUiState.Finished).place
        assertEquals("A-1", (place as PlaceChoice.Suggested).hint.building.symbol)
    }

    private fun TestScope.savedReport(): SubmissionContent {
        runCurrent()
        val route = checkNotNull(measure.uiState.value.reportFormRoute())
        val form = ReportFormViewModel { content, _, _ -> saved.add(content) }
        form.open(ReportForm(place = route.place()))
        if (route.buildingSymbol != null) form.edit(ReportEdit.Floor(ReportFloor.Numbered(0, isMezzanine = false)))
        form.edit(ReportEdit.Type(BarrierType.STAIRS_AT_ENTRANCE))
        form.edit(ReportEdit.Rating(Severity.BLOCKING))
        form.edit(ReportEdit.Photo(PendingPhotoDraft(photoId = "p1", path = "/data/p1.jpg")))
        form.save()
        runCurrent()
        assertEquals(ReportStage.SAVED, form.uiState.value?.stage)
        assertEquals(IN_A1, saved.single().location)
        return saved.single()
    }

    private companion object {
        const val ACCURACY = 6.0
        const val MEASUREMENT_MS = 31_000L
        val IN_A1 = GeoPoint(longitude = 17.0605, latitude = 51.1105)
        val CAMPUS =
            """{"type":"FeatureCollection","attribution":"© OpenStreetMap","features":[""" +
                building("A-1", west = 17.06) +
                "," +
                building("B-1", west = 17.07) +
                "]}"

        fun building(symbol: String, west: Double): String =
            """{"type":"Feature","properties":{"kind":"building","symbols":["$symbol"],"name":"$symbol",""" +
                """"osm_id":"way/$symbol"},"geometry":{"type":"Polygon","coordinates":[[[$west,51.11],""" +
                """[${west + 0.001},51.11],[${west + 0.001},51.111],[$west,51.111],[$west,51.11]]]}}"""
    }
}
