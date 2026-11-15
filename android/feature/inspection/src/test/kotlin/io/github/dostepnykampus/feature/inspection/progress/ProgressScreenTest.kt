/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.inspection.progress

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * K1 „Postęp inwentaryzacji”: stany wspólne.
 *
 * Wymagania: NFR-50
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class ProgressScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** Wczytywanie: „Wczytujemy postęp…”. */
    @Test
    fun showsLoading() {
        show(ProgressUiState.Loading)
        compose.onNodeWithText("Wczytujemy postęp…").assertIsDisplayed()
    }

    /** Bez internetu: linia, komunikat i „Spróbuj ponownie” odświeża. */
    @Test
    fun offlineOffersRetry() {
        var refreshes = 0
        show(ProgressUiState.Offline, onRefresh = { refreshes++ })
        compose.onNodeWithText("Bez internetu").assertIsDisplayed()
        compose.onNodeWithText("Ta czynność wymaga internetu", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Spróbuj ponownie").performClick()
        assertEquals("liczba ponowień", 1, refreshes)
    }

    /** Błąd serwera: komunikat i „Spróbuj ponownie”. */
    @Test
    fun serverErrorOffersRetry() {
        show(ProgressUiState.ServerError)
        compose.onNodeWithText("Serwer chwilowo nie odpowiada", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Spróbuj ponownie").assertIsDisplayed()
    }

    /** Brak uprawnień: komunikat i „Wróć do mapy”. */
    @Test
    fun forbiddenLeadsBack() {
        var back = 0
        show(ProgressUiState.Forbidden, actions = ProgressActions(onBack = { back++ }, onOpen = {}))
        compose.onNodeWithText("Nie masz już uprawnień", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Wróć do mapy").performClick()
        assertEquals("liczba powrotów", 1, back)
    }

    private fun show(
        state: ProgressUiState,
        actions: ProgressActions = ProgressActions(onBack = {}, onOpen = {}),
        onRefresh: () -> Unit = {},
    ) {
        compose.setContent {
            DostepnyKampusTheme { ProgressContent(state = state, actions = actions, onRefresh = onRefresh) }
        }
    }
}
