/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
@file:OptIn(TymczasoweRole::class)

package io.github.dostepnykampus.app.debug

import androidx.activity.ComponentActivity
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavKey
import io.github.dostepnykampus.app.AppScreen
import io.github.dostepnykampus.app.AppShell
import io.github.dostepnykampus.app.navigation.AppNavigator
import io.github.dostepnykampus.app.navigation.openReportForm
import io.github.dostepnykampus.core.data.account.TymczasoweRole
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.capture.navigation.MeasureRoute
import io.github.dostepnykampus.feature.capture.navigation.ReportFormRoute
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Osiągalność C2 z roli z przełącznika (US14.1, RO-ZBI): pasek, L2 i przejścia są prawdziwe. Mapa, pomiar i formularz
 * mają lekkie zastępniki, bo potrzebują GNSS i grafu Hilt; „Dalej” na C1 woła to samo przejście co ekran pomiaru.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class CollectorReachTest {
    /** Ekran testowy Compose. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val roles = DebugRoles()

    /** Zbierający: „Konto i praca” → „Zmierz i zgłoś barierę” → C1 → „Dalej” → C2. */
    @Test
    fun collectorReachesForm() {
        roles.account = DebugRole.COLLECTOR.account
        show()
        compose.onNodeWithText("Konto i praca").performClick()
        compose.onNodeWithText("Zmierz i zgłoś barierę").performClick()
        compose.onNodeWithText(MEASURE).assertIsDisplayed()
        compose.onNodeWithText("Dalej").performClick()
        compose.onNodeWithText(FORM).assertIsDisplayed()
    }

    /** Gość: „Konto” prowadzi do L1 bez „Zmierz i zgłoś barierę”. */
    @Test
    fun guestHasNoMeasure() {
        show()
        compose.onNodeWithText("Konto").performClick()
        compose.onNodeWithText("Zmierz i zgłoś barierę").assertDoesNotExist()
    }

    private fun show() {
        compose.setContent {
            DostepnyKampusTheme {
                AppShell(firstLaunch = false, roles = roles, onFirstHelpLeft = {}) { route -> Screen(route) }
            }
        }
    }

    @Composable
    private fun AppNavigator.Screen(route: NavKey) {
        when (route) {
            is MapRoute -> {
                Text("V1")
            }

            MeasureRoute -> {
                Text(MEASURE)
                Button(onClick = { openReportForm(PLACE) }) { Text("Dalej") }
            }

            is ReportFormRoute -> {
                Text(FORM)
            }

            else -> {
                AppScreen(route)
            }
        }
    }

    private companion object {
        const val MEASURE = "C1 Pomiar"
        const val FORM = "C2 Opisz barierę"
        val PLACE =
            ReportFormRoute(longitude = 17.06, latitude = 51.11, accuracyMeters = 0.8, buildingSymbol = "A-1")
    }
}
