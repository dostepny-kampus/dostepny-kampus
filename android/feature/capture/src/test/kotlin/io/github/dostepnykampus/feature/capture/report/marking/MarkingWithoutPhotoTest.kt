/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.report.marking

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.feature.capture.navigation.MarkingRoute
import io.github.dostepnykampus.feature.capture.photo.PhotoCamera
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.coroutines.EmptyCoroutineContext

/** C6 z modelem widoku: bez zdjęcia zmiana się nie zapisuje. Wymagania: US17.2, BR-7, BR-24 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class MarkingWithoutPhotoTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val saved = mutableListOf<SubmissionContent>()

    /**
     * Wybór „Dostępne” bez zdjęcia: ekran wskazuje brak zdjęcia, nic nie zapisuje i nie pyta o typ ani uciążliwość.
     * Scenariusz: oznaczenie-wejscia.feature: Zmiany oznaczenia wejścia nie da się zapisać bez zdjęcia
     *
     * Wymagania: US17.2, BR-7, BR-24
     */
    @Test
    fun refusesSendWithoutPhoto() {
        val viewModel =
            MarkingViewModel(
                catalog = BuildingCatalogRepository(EmptyCoroutineContext) { CATALOG },
                saver = { content, _, _ -> saved += content },
            )
        viewModel.open(MarkingRoute(entranceId = "n/1", buildingSymbol = "B-8"))
        compose.setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val camera = PhotoCamera(onTake = {})
            val actions =
                MarkingActions(camera = camera, onChoose = viewModel::choose, onSend = viewModel::send, onBack = {})
            DostepnyKampusTheme { MarkingContent(state = state, actions = actions) }
        }

        compose.onNodeWithText("Dostępne").performClick()
        compose.onNodeWithText("Wyślij do sprawdzenia").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Dodaj zdjęcie wejścia, aby wysłać zmianę.").assertIsDisplayed()
        compose.onNodeWithText("Typ bariery").assertDoesNotExist()
        compose.onNodeWithText("Uciążliwość").assertDoesNotExist()
        assertEquals("bez zdjęcia nic nie zapisano", emptyList<SubmissionContent>(), saved)
        assertEquals("zmiana czeka na zdjęcie", MarkingStage.EDITING, viewModel.uiState.value.stage)
    }

    private companion object {
        const val CATALOG =
            """{"type":"FeatureCollection","attribution":"© plik","features":[""" +
                """{"type":"Feature","properties":{"kind":"building","symbols":["B-8"],"osm_id":"way/1"},""" +
                """"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}},""" +
                """{"type":"Feature","properties":{"kind":"entrance","symbols":["B-8"],"osm_id":"n/1",""" +
                """"ref":"Wejście boczne"},"geometry":{"type":"Point","coordinates":[17.06,51.1]}}]}"""
    }
}
