/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.quick

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.dostepnykampus.app.EXPAND_HANDLE
import io.github.dostepnykampus.app.TestBuildingCard
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.feature.map.navigation.BuildingRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Zgłoszenie z karty budynku C-3 z katalogu PWr, który ma dwa wejścia bez danych. Wymagania: US4.6, BR-8 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class QuickRowReportTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app = RuntimeEnvironment.getApplication()
    private val saved = mutableListOf<SubmissionContent>()

    /**
     * Odnośnik w wierszu „2 wejścia” karty C-3 prowadzi do kroku 1 bez napisu o wejściu, a zgłoszenie dostaje
     * bieżącą pozycję telefonu, bez wejścia.
     * Scenariusz: zgloszenie-szybkie.feature: Odnośnik w wierszu kilku wejść bez danych bierze miejsce z bieżącej
     *   pozycji
     */
    @Test
    fun reportsFromRowOfEntrances() {
        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        val navigator =
            compose.showQuickApp(app.quickModels(INSIDE_D2, saved)) { route ->
                if (route is BuildingRoute) TestBuildingCard(route.symbol)
            }
        compose.runOnIdle { navigator.openBuilding(symbol = "C-3") }
        compose.waitFor { compose.onAllNodesWithText("Brak danych o wejściach").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription(EXPAND_HANDLE).performClick()
        compose.onNodeWithText("2 wejścia").assertExists("jeden wiersz kilku wejść bez danych")
        compose.onNodeWithText("Zgłoś barierę przy wejściu").performClick()
        compose.onNodeWithText("Krok 1 z 3").assertExists("krok 1 zgłoszenia szybkiego")
        compose.onAllNodesWithText("Wejście", substring = true).assertCountEquals(0)
        compose.takePhoto(navigator, app.cacheDir)
        compose.onNodeWithText("Coś innego").performClick()
        compose.waitFor { compose.onAllNodesWithText("Dołączymy", substring = true).fetchSemanticsNodes().any() }
        compose.onNodeWithText("Wyślij").assertIsEnabled().performClick()
        compose.waitFor { saved.isNotEmpty() }
        val content = saved.single()
        assertEquals("bieżąca pozycja telefonu", INSIDE_D2, content.location)
        assertEquals("dokładność odczytu pozycji", FIX_ACCURACY, content.accuracyMeters)
        assertNull("bez wejścia", content.entranceId)
    }
}
