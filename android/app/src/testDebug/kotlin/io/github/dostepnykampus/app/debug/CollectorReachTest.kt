/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
@file:OptIn(TymczasoweRole::class)

package io.github.dostepnykampus.app.debug

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
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
import io.github.dostepnykampus.feature.capture.measure.place
import io.github.dostepnykampus.feature.capture.ReportEdit
import io.github.dostepnykampus.feature.capture.ReportForm
import io.github.dostepnykampus.feature.capture.ReportFormViewModel
import io.github.dostepnykampus.feature.capture.navigation.MeasureRoute
import io.github.dostepnykampus.feature.capture.navigation.ReportFormRoute
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Osiągalność C2 z roli z przełącznika: pasek, L2 i przejścia są prawdziwe. Mapa, pomiar i formularz
 * mają lekkie zastępniki, bo potrzebują GNSS i grafu Hilt; „Dalej” na C1 woła to samo przejście co ekran pomiaru.
 *
 * Wymagania: US14.1, RO-ZBI
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class CollectorReachTest {
    /** Ekran testowy Compose. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val roles = DebugRoles()

    /**
     * Zbierający wraca z C1 do C2 po odtworzeniu stanu i zaczyna od zera po opuszczeniu pomiaru.
     *
     * Wymagania: US7.4, NFR-7
     */
    @Test
    fun draftSurvivesStateRestore() {
        roles.switchTo(DebugRole.COLLECTOR.account)
        val restoration = StateRestorationTester(compose)
        show(restoration)

        compose.onNodeWithText("Konto i praca").performClick()
        openForm()
        compose.onNodeWithText("Wpisz opis").performClick()
        compose.onNodeWithText(CHANGE_PLACE).performClick()
        compose.onNodeWithText(MEASURE).assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()

        compose.onNodeWithText(NEXT).performClick()
        compose.onNodeWithText(DRAFT_TEXT).assertIsDisplayed()
        compose.onNodeWithText(CHANGE_PLACE).performClick()
        compose.onNodeWithText("Opuść pomiar").performClick()
        openForm()
        compose.onNodeWithText(DRAFT_TEXT).assertDoesNotExist()
    }

    /** Gość nie widzi wejścia do pomiaru. Wymagania: US14.1 */
    @Test
    fun guestHasNoMeasure() {
        show()
        compose.onNodeWithText("Konto").performClick()
        compose.onNodeWithText(MEASURE_ACTION).assertDoesNotExist()
    }

    private fun openForm() {
        compose.onNodeWithText(MEASURE_ACTION).performClick()
        compose.onNodeWithText(MEASURE).assertIsDisplayed()
        compose.onNodeWithText(NEXT).performClick()
        compose.onNodeWithText(FORM).assertIsDisplayed()
    }

    private fun show(restoration: StateRestorationTester = StateRestorationTester(compose)) {
        restoration.setContent {
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
                Column {
                    Text(MEASURE)
                    Button(onClick = { openReportForm(PLACE) }) { Text(NEXT) }
                    Button(onClick = { back() }) { Text("Opuść pomiar") }
                }
            }

            is ReportFormRoute -> {
                DraftForm(route)
            }

            else -> {
                AppScreen(route)
            }
        }
    }

    @Composable
    private fun AppNavigator.DraftForm(route: ReportFormRoute) {
        val viewModel = remember { ReportFormViewModel { _, _, _ -> } }
        SideEffect { viewModel.open(ReportForm(place = route.place()), reportDraft) }
        val state by viewModel.uiState.collectAsState()
        Column {
            Text(FORM)
            Text(state?.run { form.description }.orEmpty())
            Button(onClick = { viewModel.edit(ReportEdit.Description(DRAFT_TEXT)) }) { Text("Wpisz opis") }
            Button(
                onClick = {
                    viewModel.changePlace()
                    back()
                },
            ) { Text(CHANGE_PLACE) }
        }
    }

    private companion object {
        const val CHANGE_PLACE = "Zmień miejsce"
        const val NEXT = "Dalej"
        const val MEASURE_ACTION = "Zmierz i zgłoś barierę"
        const val DRAFT_TEXT = "Próg przy drzwiach"
        const val MEASURE = "C1 Pomiar"
        const val FORM = "C2 Opisz barierę"
        val PLACE =
            ReportFormRoute(longitude = 17.06, latitude = 51.11, accuracyMeters = 0.8, buildingSymbol = "A-1")
    }
}
