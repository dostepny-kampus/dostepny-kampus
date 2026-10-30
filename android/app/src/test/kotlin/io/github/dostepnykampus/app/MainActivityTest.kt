/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.dostepnykampus.core.data.start.firstLaunchRepository
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Aktywność z oboczką w Robolectric: pierwsze uruchomienie, odtworzenie aktywności i zapis instrukcji. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class MainActivityTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun pressBack() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    // Scenariusz (częściowo): przegladanie-mapy.feature: Instrukcja przy pierwszym uruchomieniu
    @Test
    fun us5_1_first_launch_recreate() {
        compose.waitUntil { compose.onAllNodesWithText("Pomoc").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Pomoc").assertIsSelected()
        pressBack()
        compose.waitUntil { runBlocking { !compose.activity.firstLaunchRepository().isFirstLaunch() } }
        compose.onNodeWithText("Pomoc").performClick()
        compose.activityRule.scenario.recreate()
        compose.waitUntil { compose.onAllNodesWithText("Pomoc").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Pomoc").assertIsSelected()
        pressBack()
        compose.onNodeWithText("Mapa").assertIsSelected()
    }
}
