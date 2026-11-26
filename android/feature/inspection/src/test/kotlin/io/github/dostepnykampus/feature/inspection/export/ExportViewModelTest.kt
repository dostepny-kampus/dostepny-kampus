/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.inspection.export

import io.github.dostepnykampus.core.data.export.BarrierExport
import io.github.dostepnykampus.core.data.export.ExportOutcome
import io.github.dostepnykampus.core.data.export.ExportPlace
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Stan K3: wybór formatu, przygotowanie pliku i wynik pobrania.
 *
 * Wymagania: US13, US13.1, US13.2, NFR-50, NFR-41
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExportViewModelTest {
    private val asked = mutableListOf<ExportFormat>()
    private val places = mutableListOf<ExportPlace>()

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
    fun downloadsWithoutPlace() =
        exportTest(ExportOutcome.Saved(isEmpty = false)) { viewModel ->
            assertFalse(viewModel.isPlaceAsked, "od Androida 10 bez okna wyboru miejsca")
            viewModel.download()
            runCurrent()
            assertEquals(listOf<ExportPlace>(ExportPlace.Downloads), places, "zapis w „Pobrane”")
        }

    @Test
    fun savesToChosenPlace() =
        exportTest(ExportOutcome.Saved(isEmpty = false), hasDownloadsFolder = false) { viewModel ->
            assertTrue(viewModel.isPlaceAsked, "na Androidzie 8–9 okno wyboru miejsca")
            viewModel.choose(ExportFormat.CSV)
            assertEquals("eksport.csv", viewModel.suggestedName(), "podpowiedziana nazwa w wybranym formacie")
            viewModel.downloadTo(URI)
            runCurrent()
            assertEquals(listOf<ExportPlace>(ExportPlace.Chosen(URI)), places, "zapis w wybranym miejscu")
            assertEquals(ExportStage.SAVED_CHOSEN, viewModel.state.value.stage, "plik w wybranym miejscu")
        }

    @Test
    fun chosenEmptyFileIsEmpty() =
        exportTest(ExportOutcome.Saved(isEmpty = true), hasDownloadsFolder = false) { viewModel ->
            viewModel.downloadTo(URI)
            runCurrent()
            assertEquals(ExportStage.EMPTY, viewModel.state.value.stage, "plik bez barier")
        }

    @Test
    fun ignoresSecondTap() =
        exportTest(ExportOutcome.Saved(isEmpty = false)) { viewModel ->
            viewModel.download()
            viewModel.download()
            runCurrent()
            assertEquals(1, asked.size, "jedno pobranie naraz")
        }

    private fun exportTest(
        outcome: ExportOutcome,
        hasDownloadsFolder: Boolean = true,
        body: suspend TestScope.(ExportViewModel) -> Unit,
    ) {
        val export =
            object : BarrierExport {
                override val hasDownloadsFolder: Boolean = hasDownloadsFolder

                override fun fileName(format: ExportFormat): String = "eksport.${format.extension}"

                override suspend fun download(format: ExportFormat, place: ExportPlace): ExportOutcome {
                    asked += format
                    places += place
                    return outcome
                }
            }
        val dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        try {
            runTest(dispatcher) { body(ExportViewModel(export)) }
        } finally {
            Dispatchers.resetMain()
        }
    }

    private companion object {
        const val URI = "content://dokumenty/eksport.csv"
    }
}
