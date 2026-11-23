/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.grounds

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.dostepnykampus.app.quick.quickModels
import io.github.dostepnykampus.app.quick.showQuickApp
import io.github.dostepnykampus.app.quick.takePhoto
import io.github.dostepnykampus.app.quick.waitFor
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.submission.BarrierGroup
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.feature.capture.navigation.ReportRoute
import io.github.dostepnykampus.feature.capture.quick.QuickLookup
import io.github.dostepnykampus.feature.capture.quick.QuickSendViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Zgłoszenie szybkie na prawdziwym katalogu i granicach terenów PWr. Wymagania: US4.9, BR-G5 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class QuickOnGroundsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app = RuntimeEnvironment.getApplication()
    private val saved = mutableListOf<SubmissionContent>()

    /**
     * Przerwa 60 m między budynkami P-20 i P-23 terenu Wittigowo: zgłoszenie idzie spoza budynku.
     * Scenariusz (przykład: w przerwie 60 m między dwoma budynkami tego samego terenu): zgloszenie-szybkie.feature:
     *   Zgłoszenie szybkie tylko na terenie PWr
     */
    @Test
    fun sendsFromGapBetweenBuildings() {
        assertSentOutsideBuilding(GAP)
    }

    /**
     * Zapas terenu Wittigowo 20 m za obszarem uczelni: zgłoszenie idzie spoza budynku.
     * Scenariusz (przykład: 20 m za obszarem uczelni, w zapasie terenu): zgloszenie-szybkie.feature:
     *   Zgłoszenie szybkie tylko na terenie PWr
     */
    @Test
    fun sendsFromMarginBehindArea() {
        assertSentOutsideBuilding(MARGIN)
    }

    /**
     * Punkt 20 m na zachód od zachodniego krańca granic terenów z `pwr-areas.geojson`: napis o terenie PWr,
     * bez „Wyślij” i bez zapisu zgłoszenia.
     * Scenariusz (przykład: 20 m za granicą terenu PWr): zgloszenie-szybkie.feature:
     *   Zgłoszenie szybkie tylko na terenie PWr
     */
    @Test
    fun refusesBeyondBoundary() {
        reportFrom(BEYOND_BOUNDARY)
        compose.onNodeWithText("Jesteś poza terenem PWr", substring = true).assertExists("napis o terenie")
        compose.onNodeWithText("Wyślij").assertDoesNotExist()
        assertTrue("zgłoszenie nie zostaje zapisane", saved.isEmpty())
    }

    private fun assertSentOutsideBuilding(point: GeoPoint) {
        val where = reportFrom(point)
        assertFalse("miejsce poza budynkiem", (where.lookup.value as QuickLookup.Found).position.isInBuilding)
        compose.onNodeWithText("Wyślij").assertIsEnabled().performClick()
        compose.waitFor { saved.isNotEmpty() }
        assertEquals("miejsce zgłoszenia", point, saved.single().location)
        assertNull("bez piętra poza budynkiem", saved.single().floor)
    }

    private fun reportFrom(point: GeoPoint): QuickSendViewModel {
        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        val models = app.quickModels(point, saved)
        val navigator = compose.showQuickApp(models)
        navigator.report()
        assertTrue("zalogowany wchodzi w zgłoszenie, nie w logowanie", navigator.current is ReportRoute)
        compose.takePhoto(navigator, app.cacheDir)
        navigator.nextReportStep((navigator.current as ReportRoute).copy(group = BarrierGroup.HORIZONTAL_CIRCULATION))
        compose.onNodeWithText("Krok 3 z 3").assertExists("krok 3 po wyborze grupy „komunikacja pozioma”")
        val where = checkNotNull(models.send)
        compose.waitFor { where.lookup.value !is QuickLookup.Locating }
        return where
    }

    private companion object {
        val GAP = GeoPoint(longitude = 17.0889701, latitude = 51.1013186)
        val MARGIN = GeoPoint(longitude = 17.0881108, latitude = 51.101438)
        val BEYOND_BOUNDARY = GeoPoint(longitude = 17.0082293, latitude = 51.1269098)
    }
}
