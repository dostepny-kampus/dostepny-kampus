/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.inspection.export

import io.github.dostepnykampus.core.data.export.ExportOutcome
import io.github.dostepnykampus.core.model.export.ExportFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Stan K3: wybór formatu, przygotowanie pliku i wynik pobrania.
 *
 * Wymagania: US13, US13.1, US13.2, NFR-50
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExportViewModelTest {
    private val asked = mutableListOf<ExportFormat>()

    @Test
    fun startsWithGeoJson() =
        exportTest(ExportOutcome.Saved(isEmpty = false)) { viewModel ->
            assertEquals(ExportUiState(), viewModel.state.value, "GeoJSON wybrany, bez wyniku")
        }

    @Test
    fun savesChosenFormat() =
        exportTest(ExportOutcome.Saved(isEmpty = false)) { viewModel ->
            viewModel.choose(ExportFormat.CSV)
            viewModel.download()
            assertEquals(ExportStage.LOADING, viewModel.state.value.stage, "przygotowanie pliku")
            runCurrent()
            assertEquals(ExportStage.SAVED, viewModel.state.value.stage, "plik zapisany")
            assertEquals(listOf(ExportFormat.CSV), asked, "pobrany wybrany format")
        }

    @Test
    fun showsEmptyFile() =
        exportTest(ExportOutcome.Saved(isEmpty = true)) { viewModel ->
            viewModel.download()
            runCurrent()
            assertEquals(ExportStage.EMPTY, viewModel.state.value.stage, "plik bez barier")
        }

    @Test
    fun mapsFailures() {
        val expected =
            mapOf(
                ExportOutcome.Offline to ExportStage.OFFLINE,
                ExportOutcome.Forbidden to ExportStage.FORBIDDEN,
                ExportOutcome.Failed to ExportStage.SERVER_ERROR,
            )
        expected.forEach { (outcome, stage) ->
            exportTest(outcome) { viewModel ->
                viewModel.download()
                runCurrent()
                assertEquals(stage, viewModel.state.value.stage, "wynik $outcome")
            }
        }
    }

    @Test
    fun ignoresSecondTap() =
        exportTest(ExportOutcome.Saved(isEmpty = false)) { viewModel ->
            viewModel.download()
            viewModel.download()
            runCurrent()
            assertEquals(1, asked.size, "jedno pobranie naraz")
        }

    private fun exportTest(outcome: ExportOutcome, body: suspend TestScope.(ExportViewModel) -> Unit) {
        val dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        try {
            runTest(dispatcher) {
                body(ExportViewModel { format -> outcome.also { asked += format } })
            }
        } finally {
            Dispatchers.resetMain()
        }
    }
}
