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
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.github.dostepnykampus.core.data.start.FirstLaunch
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.inject.Inject

/**
 * Aktywność z oboczką w Robolectric na grafie Hilt aplikacji: pierwsze uruchomienie, odtworzenie aktywności i zapis
 * instrukcji.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, qualifiers = "w360dp-h800dp")
class MainActivityTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    // Ten sam zapis pierwszego uruchomienia, który dostała aktywność z grafu Hilt.
    @Inject
    lateinit var firstLaunch: FirstLaunch

    @Before
    fun inject() {
        hilt.inject()
    }

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
        compose.waitUntil { runBlocking { !firstLaunch.isFirstLaunch() } }
        compose.onNodeWithText("Pomoc").performClick()
        compose.activityRule.scenario.recreate()
        compose.waitUntil { compose.onAllNodesWithText("Pomoc").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Pomoc").assertIsSelected()
        pressBack()
        compose.onNode(MapTab).assertIsSelected()
    }
}
