/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.quick.floor

import android.Manifest
import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import io.github.dostepnykampus.core.location.PositionFix
import io.github.dostepnykampus.core.location.PositionSource
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.submission.BarrierGroup
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionFloor
import io.github.dostepnykampus.feature.capture.navigation.ReportRoute
import io.github.dostepnykampus.feature.capture.navigation.ReportStep
import io.github.dostepnykampus.feature.capture.quick.QUICK_PWR_FLOORS
import io.github.dostepnykampus.feature.capture.quick.QuickLookup
import io.github.dostepnykampus.feature.capture.quick.QuickReportActions
import io.github.dostepnykampus.feature.capture.quick.QuickSendViewModel
import io.github.dostepnykampus.feature.capture.quick.quickPwrFloors
import io.github.dostepnykampus.feature.capture.quick.quickSendModel
import io.github.dostepnykampus.feature.capture.quick.showQuickReport
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Ekran V4: pytanie o piętro w kroku 2 zależy od budynku i grupy, a odpowiedź trafia do wysłanego zgłoszenia.
 *
 * Wymagania: US4.5, BR-14
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class QuickFloorScenarioTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val routes = mutableListOf<ReportRoute>()
    private val submissions = mutableListOf<SubmissionContent>()
    private val current = mutableStateOf(ReportRoute())
    private val actions =
        QuickReportActions(onNext = { routes.add(it) }, onSent = {}, onBack = {}, onBackToMap = {})
    private val atGroup = ReportRoute(step = ReportStep.GROUP, photoId = "zdjecie-1", photoPath = "/zdjecie-1.jpg")

    /**
     * W budynku D-2 przy grupie „Schody i windy” krok 2 pyta o piętro z „Nie wiem” i etykietami z danych PWr.
     * Scenariusz (przykład: w budynku D-2): zgloszenie-szybkie.feature: Pytanie o piętro tylko w budynku i przy
     *   grupie, która go wymaga
     *
     * Wymagania: US4.5, BR-14
     */
    @Test
    fun asksFloorInD2() {
        standAt(D2)
        compose.onNodeWithText("Schody i windy").performClick()
        assertFloorAnswers(requireNotNull(QUICK_PWR_FLOORS["D-2"]))
        compose.onNodeWithText("10").assertDoesNotExist()
    }

    /**
     * W budynku B-4 przy grupie „Toalety” krok 2 pyta o piętro z „Nie wiem” i przyciskami „Parter”, „1”, „2”.
     * Scenariusz (przykład: w budynku B-4): zgloszenie-szybkie.feature: Pytanie o piętro tylko w budynku i przy
     *   grupie, która go wymaga
     *
     * Wymagania: US4.5, BR-14
     */
    @Test
    fun asksFloorInB4() {
        standAt(B4)
        compose.onNodeWithText("Toalety").performClick()
        assertFloorAnswers(listOf("Parter", "1", "2"))
    }

    /**
     * W budynku C-13 przy grupie „Wejście i drzwi” krok 2 pyta o piętro z „Nie wiem” i etykietami z danych PWr.
     * Scenariusz (przykład: w budynku C-13, przy wejściu): zgloszenie-szybkie.feature: Pytanie o piętro tylko
     *   w budynku i przy grupie, która go wymaga
     *
     * Wymagania: US4.5, BR-14
     */
    @Test
    fun asksFloorInC13() {
        standAt(C13)
        compose.onNodeWithText("Wejście i drzwi").performClick()
        assertFloorAnswers(requireNotNull(QUICK_PWR_FLOORS["C-13"]))
        compose.onNodeWithText("10").assertDoesNotExist()
    }

    /**
     * W budynku C-13 przy grupie „Coś innego” krok 2 przechodzi do kroku 3 bez pytania o piętro.
     * Scenariusz (przykład: w budynku C-13): zgloszenie-szybkie.feature: Pytanie o piętro tylko w budynku i przy
     *   grupie, która go wymaga
     *
     * Wymagania: US4.5, BR-14
     */
    @Test
    fun skipsFloorForOtherInC13() {
        standAt(C13)
        compose.onNodeWithText("Coś innego").performClick()
        assertSkipsFloor(BarrierGroup.OTHER)
    }

    /**
     * Na chodniku przed A-1, poza konturem, grupa „Przejścia i chodniki” przechodzi do kroku 3 bez pytania o piętro.
     * Scenariusz (przykład: na chodniku przed A-1, poza konturem): zgloszenie-szybkie.feature: Pytanie o piętro
     *   tylko w budynku i przy grupie, która go wymaga
     *
     * Wymagania: US4.5, BR-14
     */
    @Test
    fun skipsFloorOutsideContour() {
        standAt(BEFORE_A1)
        compose.onNodeWithText("Przejścia i chodniki").performClick()
        assertSkipsFloor(BarrierGroup.HORIZONTAL_CIRCULATION)
    }

    /**
     * W budynku D-2 odpowiedź „Nie wiem” zostawia piętro puste, a zgłoszenie z budynku D-2 powstaje po 3 krokach.
     * Scenariusz: zgloszenie-szybkie.feature: Odpowiedź „Nie wiem” zostawia piętro puste i pozwala wysłać
     *   zgłoszenie
     *
     * Wymagania: US4.5, BR-14
     */
    @Test
    fun leavesFloorEmptyInD2() {
        val model = standAt(D2)
        compose.onNodeWithText("Schody i windy").performClick()
        compose.onNodeWithText("Na którym piętrze?").assertIsDisplayed()
        compose.onNodeWithText("Nie wiem").performClick()
        sendFromStepThree()
        assertEquals("budynek D-2", "D-2", model.building())
        assertEquals("miejsce w budynku D-2", listOf(D2), submissions.map { it.location })
        assertNull("piętro puste", submissions.single().floor)
        assertEquals("jedno przejście z kroku 2 do kroku 3", 1, routes.size)
    }

    /**
     * W budynku B-4 wybrane piętro 2 trafia do zgłoszenia z budynku B-4.
     * Scenariusz: zgloszenie-szybkie.feature: Wybrane piętro trafia do zgłoszenia
     *
     * Wymagania: US4.5, BR-14
     */
    @Test
    fun sendsChosenFloorInB4() {
        val model = standAt(B4)
        compose.onNodeWithText("Toalety").performClick()
        compose.onNodeWithText("Na którym piętrze?").assertIsDisplayed()
        compose.onNodeWithText("2").performClick()
        sendFromStepThree()
        assertEquals("budynek B-4", "B-4", model.building())
        assertEquals("miejsce w budynku B-4", listOf(B4), submissions.map { it.location })
        assertEquals("piętro 2", SubmissionFloor(number = 2), submissions.single().floor)
    }

    /**
     * Poza terenem PWr grupa „Schody i windy” przechodzi do kroku 3 bez pytania o piętro.
     *
     * Wymagania: US4.5, BR-14
     */
    @Test
    fun skipsFloorOutsideGrounds() {
        val model = standAt(OUTSIDE_PWR)
        compose.waitUntil(timeoutMillis = WAIT_MS) { model.lookup.value == QuickLookup.Outside }
        compose.onNodeWithText("Schody i windy").performClick()
        assertSkipsFloor(BarrierGroup.VERTICAL_CIRCULATION)
    }

    private fun standAt(point: GeoPoint): QuickSendViewModel {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        val positions = PositionSource { flowOf(PositionFix(point, accuracyMeters = 6.0, elapsedMillis = 0L)) }
        val model =
            quickSendModel(
                positions = positions,
                saver = { content, _, _ -> submissions.add(content) },
                floors = quickPwrFloors(),
            )
        current.value = atGroup
        compose.showQuickReport(route = current, actions = actions, send = model)
        compose.waitForIdle()
        return model
    }

    private fun assertFloorAnswers(labels: List<String>) {
        val last = hasText(labels.last())
        compose.waitUntil(timeoutMillis = WAIT_MS) { compose.onAllNodes(last).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Krok 2 z 3").assertIsDisplayed()
        compose.onNodeWithText("Na którym piętrze?").assertIsDisplayed()
        compose.onNodeWithText("Nie wiem").assertIsDisplayed()
        labels.forEach { compose.onNodeWithText(it).assertIsDisplayed() }
        assertEquals("krok 2 trwa, bez przejścia dalej", emptyList<ReportRoute>(), routes)
    }

    private fun assertSkipsFloor(group: BarrierGroup) {
        compose.onNodeWithText("Krok 2 z 3").assertIsDisplayed()
        compose.onNodeWithText("Na którym piętrze?").assertDoesNotExist()
        assertEquals("przejście do kroku 3 bez piętra", listOf(atGroup.copy(group = group)), routes)
    }

    private fun sendFromStepThree() {
        current.value = routes.single().copy(step = ReportStep.SEND)
        compose.waitForIdle()
        compose.onNodeWithText("Krok 3 z 3").assertIsDisplayed()
        compose.onNodeWithText("Wyślij").performClick()
        compose.waitUntil(timeoutMillis = WAIT_MS) { submissions.isNotEmpty() }
    }

    private fun QuickSendViewModel.building(): String? = (lookup.value as QuickLookup.Found).position.buildingSymbol

    private companion object {
        const val WAIT_MS = 5_000L
        val D2 = GeoPoint(longitude = 17.0568, latitude = 51.10985)
        val B4 = GeoPoint(longitude = 17.0653, latitude = 51.10815)
        val C13 = GeoPoint(longitude = 17.0597, latitude = 51.1076)
        val BEFORE_A1 = GeoPoint(longitude = 17.059, latitude = 51.109)
        val OUTSIDE_PWR = GeoPoint(longitude = 17.0082293, latitude = 51.1269098)
    }
}
