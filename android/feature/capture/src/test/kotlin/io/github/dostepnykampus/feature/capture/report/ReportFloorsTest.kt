/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.report

import io.github.dostepnykampus.core.data.buildings.BuildingFloors
import io.github.dostepnykampus.core.data.submissions.PendingPhotoDraft
import io.github.dostepnykampus.core.model.BarrierType
import io.github.dostepnykampus.core.model.Floor
import io.github.dostepnykampus.core.model.FloorList
import io.github.dostepnykampus.core.model.Severity
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.core.model.SubmissionFloor
import io.github.dostepnykampus.feature.capture.measure.place
import io.github.dostepnykampus.feature.capture.quick.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Piętra z planu PWr w formularzu C2: pobranie po otwarciu i wpisanie piętra bez planu. Wymagania: US7.2, BR-G1 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReportFloorsTest {
    private val dispatcher = StandardTestDispatcher()
    private val saved = mutableListOf<SubmissionContent>()
    private val asked = mutableListOf<String>()

    @BeforeEach
    fun main() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun reset() {
        Dispatchers.resetMain()
    }

    @Test
    fun `us7_2 piętra z planu`() =
        runTest(dispatcher) {
            val viewModel = opened(PLAN)
            runCurrent()

            val floors = state(viewModel).form.place.floors
            assertEquals(listOf("A-1-p", "A-1-0"), floors.items.map(PlanFloor::id))
            assertEquals(listOf("Piwnica", "Parter"), floors.items.map(PlanFloor::label))
            assertEquals("Politechnika Wrocławska", floors.source)
            assertEquals(listOf("A-1"), asked)
            viewModel.back()
            assertEquals(ReportStage.LEFT, state(viewModel).stage)
        }

    @Test
    fun `us7_2 zapis z piętrem z planu`() =
        runTest(dispatcher) {
            val viewModel = opened(PLAN)
            runCurrent()
            viewModel.edit(ReportEdit.Floor(ReportFloor.Planned("A-1-p")))
            viewModel.edit(ReportEdit.Type(BarrierType.THRESHOLD))
            viewModel.edit(ReportEdit.Rating(Severity.ONLY_WITH_HELP))
            viewModel.edit(ReportEdit.Photo(PendingPhotoDraft(photoId = "p1", path = "/data/p1.jpg")))

            viewModel.save()
            runCurrent()

            assertEquals(SubmissionFloor(floorId = "A-1-p"), saved.single().floor)
        }

    @Test
    fun `us7_2 wpisane piętro znika`() =
        runTest(dispatcher) {
            val viewModel = opened(PLAN)
            viewModel.edit(ReportEdit.Floor(ReportFloor.Numbered(number = 2, isMezzanine = false)))
            runCurrent()

            assertNull(state(viewModel).form.floor)
        }

    @Test
    fun `br_g1 budynek podstawowy`() =
        runTest(dispatcher) {
            val basic = opened(FloorList(source = "Politechnika Wrocławska", floors = emptyList()))
            val offline = opened(null)
            runCurrent()

            assertEquals(PlanFloors(emptyList()), state(basic).form.place.floors)
            assertEquals(PlanFloors(emptyList()), state(offline).form.place.floors)
        }

    @Test
    fun `br_g4 poza budynkiem`() =
        runTest(dispatcher) {
            opened(PLAN, place = A1.copy(buildingSymbol = null))
            runCurrent()

            assertEquals(emptyList<String>(), asked)
        }

    private fun opened(answer: FloorList?, place: ReportPlace = A1): ReportFormViewModel {
        val floors =
            BuildingFloors { symbol ->
                asked += symbol
                answer
            }
        return ReportFormViewModel(floors = floors) { content, _, _ -> saved += content }
            .apply { open(ReportForm(place = place)) }
    }

    private fun state(viewModel: ReportFormViewModel): ReportUiState = checkNotNull(viewModel.uiState.value)

    private companion object {
        val PLAN =
            FloorList(
                source = "Politechnika Wrocławska",
                floors =
                    listOf(
                        Floor(id = "A-1-p", label = "Piwnica", order = -1),
                        Floor(id = "A-1-0", label = "Parter", order = 0),
                    ),
            )
    }
}
