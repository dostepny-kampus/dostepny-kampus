/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
@file:OptIn(TymczasoweRole::class)

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
import io.github.dostepnykampus.core.data.account.GuestRoleProvider
import io.github.dostepnykampus.core.data.account.TymczasoweRole
import io.github.dostepnykampus.feature.map.navigation.BuildingRoute
import io.github.dostepnykampus.feature.map.navigation.EntrancesRoute
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val CHECK = "Sprawdź, którędy wejść"
private const val VISIT = "Którędy wejść do A-1"

// Klatka zegara Compose: przejście V2 → V3 już trwa, ale się nie skończyło.
private const val MID_TRANSITION_MILLIS = 48L

/**
 * „Wstecz” naciśnięte, gdy V3 jest już w drzewie semantyki, a przejście sceny V2 → V3 jeszcze trwa (zegar Compose
 * zatrzymany). Tak robi test emulatora `CampusPathDeviceTest.us3_search_card_entrances`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class BackDuringTransitionTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Composable
    private fun Shell() {
        AppShell(firstLaunch = false, roles = GuestRoleProvider, onFirstHelpLeft = {}) { route ->
            val map = Modifier.fillMaxSize().clickable { openBuilding(symbol = "A-1") }
            when (route) {
                is MapRoute -> Text(text = "A-1", modifier = map)
                is BuildingRoute -> Text(text = CHECK, modifier = Modifier.clickable { openEntrances(symbol = "A-1") })
                is EntrancesRoute -> Text(text = VISIT)
                else -> Unit
            }
        }
    }

    /**
     * Wiersz: V2 → „Sprawdź, którędy wejść” → V3; „Wstecz” w trakcie przejścia, gdy V3 jest już widoczne → po końcu
     * przejścia V2, bez V3; kolejne „Wstecz” → V1, US3, US1.1.
     */
    @Ignore("znikający arkusz V2 przechwytuje „Wstecz” w trakcie przejścia; włączany razem z poprawką")
    @Test
    fun us3_back_mid_transition() {
        compose.setContent { Shell() }
        compose.onNodeWithText("A-1").performClick()
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText(CHECK).performClick()
        compose.mainClock.advanceTimeBy(MID_TRANSITION_MILLIS)
        compose.onNodeWithText(VISIT).assertExists("V3 w drzewie semantyki w trakcie przejścia")
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

    /** Wiersz kontrolny: V2 → V3, „Wstecz” po końcu przejścia → V2; kolejne „Wstecz” → V1, US3, US1.1. */
    @Test
    fun us3_back_after_transition() {
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
