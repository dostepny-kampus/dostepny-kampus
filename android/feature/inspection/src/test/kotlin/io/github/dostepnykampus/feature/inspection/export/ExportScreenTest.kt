/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.inspection.export

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.export.ExportFormat
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * K3 „Pobierz eksport”: wybór formatu, pobranie i wyniki.
 *
 * Wymagania: US13, US13.1, US13.2, US13.3, NFR-50
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class ExportScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val events = mutableListOf<String>()

    private val state = mutableStateOf(ExportUiState())

    /** Ekran K3 ze stanem, który test zmienia. */
    @Before
    fun setUp() {
        compose.setContent {
            DostepnyKampusTheme {
                ExportContent(
                    state = state.value,
                    actions = ExportActions(onBack = { events += "back" }, onLeave = { events += "leave" }),
                    onChoose = { format -> events += format.name },
                    onDownload = { events += "download" },
                )
            }
        }
    }

    /** Zwykły stan: formaty i opis zawartości; wybór CSV i „Pobierz plik”. */
    @Test
    fun choosesCsvAndDownloads() {
        compose.onNodeWithText(ABOUT).assertIsDisplayed()
        compose.onNodeWithText("CSV (PUWG 1992, EPSG:2180)").performClick()
        compose.onNodeWithText("Pobierz plik").performClick()
        assertEquals("wybór i pobranie", listOf("CSV", "download"), events)
    }

    /** Wyniki pobrania: zapisany w „Pobrane” albo w wybranym miejscu, pusty i przygotowanie. */
    @Test
    fun showsResults() {
        show(ExportUiState(stage = ExportStage.SAVED))
        compose.onNodeWithText("Plik zapisany w pobranych plikach.").assertIsDisplayed()
        show(ExportUiState(stage = ExportStage.SAVED_CHOSEN))
        compose.onNodeWithText("Plik zapisany w wybranym miejscu.").assertIsDisplayed()
        show(ExportUiState(stage = ExportStage.EMPTY))
        compose.onNodeWithText("Pobrany plik będzie pusty.").assertIsDisplayed()
        show(ExportUiState(format = ExportFormat.CSV, stage = ExportStage.LOADING))
        compose.onNodeWithText("Przygotowujemy plik…").assertIsDisplayed()
    }

    /** Brak sieci ponawia pobranie, a brak uprawnień wraca do mapy. */
    @Test
    fun handlesFailures() {
        show(ExportUiState(stage = ExportStage.OFFLINE))
        compose.onNodeWithText("Spróbuj ponownie").performClick()
        show(ExportUiState(stage = ExportStage.FORBIDDEN))
        compose.onNodeWithText("Wróć do mapy").performClick()
        assertEquals("ponowienie i mapa", listOf("download", "leave"), events)
    }

    private fun show(next: ExportUiState) {
        compose.runOnIdle { state.value = next }
    }

    private companion object {
        const val ABOUT = "Plik zawiera tylko obecne, sprawdzone bariery, bez danych autorów."
    }
}
