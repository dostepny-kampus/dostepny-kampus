/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.dostepnykampus.app.navigation.SignInRoute
import io.github.dostepnykampus.feature.map.navigation.BuildingRoute
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val CHECK = "Zgłoś barierę"
private const val VISIT = "Logowanie"

private const val MID_TRANSITION_MILLIS = 48L

/**
 * „Wstecz” naciśnięte, gdy ekran nad V2 (L1 po „Zgłoś barierę” Gościa) jest już w drzewie semantyki, a przejście
 * sceny jeszcze trwa (zegar Compose zatrzymany). `MID_TRANSITION_MILLIS` to klatka zegara Compose, w której
 * przejście V2 → L1 już trwa, ale się nie skończyło.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class BackDuringTransitionTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Composable
    private fun Shell() {
        AppShell(firstLaunch = false, roles = TestRoles(), onFirstHelpLeft = {}) { route ->
            val map = Modifier.fillMaxSize().clickable { openBuilding(symbol = "A-1") }
            when (route) {
                is MapRoute -> Text(text = "A-1", modifier = map)
                is BuildingRoute -> Text(text = CHECK, modifier = Modifier.clickable { report() })
                is SignInRoute -> Text(text = VISIT)
                else -> Unit
            }
        }
    }

    /**
     * Wiersz: V2 → „Zgłoś barierę” Gościa → L1; „Wstecz” w trakcie przejścia, gdy L1 jest już widoczne → po końcu
     * przejścia V2, bez L1; kolejne „Wstecz” → V1.
     *
     * Wymagania: US14.2, US1.1
     */
    @Test
    fun goesBackMidTransition() {
        compose.setContent { Shell() }
        compose.onNodeWithText("A-1").performClick()
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText(CHECK).performClick()
        compose.mainClock.advanceTimeBy(MID_TRANSITION_MILLIS)
        compose.onNodeWithText(VISIT).assertExists("L1 w drzewie semantyki w trakcie przejścia")
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        compose.onNodeWithText(VISIT).assertDoesNotExist()
        compose.onNodeWithText(CHECK).assertExists("„Wstecz” w trakcie przejścia nie wróciło do V2")
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        compose.onNodeWithText(CHECK).assertDoesNotExist()
        compose.onNodeWithText("A-1").assertExists("drugie „Wstecz” nie wróciło do V1")
    }

    /** Wiersz kontrolny: V2 → L1, „Wstecz” po końcu przejścia → V2; kolejne „Wstecz” → V1. Wymagania: US14.2, US1.1 */
    @Test
    fun goesBackAfterTransition() {
        compose.setContent { Shell() }
        compose.onNodeWithText("A-1").performClick()
        compose.waitForIdle()
        compose.onNodeWithText(CHECK).performClick()
        compose.waitForIdle()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        compose.onNodeWithText(VISIT).assertDoesNotExist()
        compose.onNodeWithText(CHECK).assertExists("„Wstecz” po przejściu nie wróciło do V2")
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        compose.onNodeWithText(CHECK).assertDoesNotExist()
    }
}
