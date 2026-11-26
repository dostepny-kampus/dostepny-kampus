/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.systemtest

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnySibling
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.intent.Intents
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.dostepnykampus.app.MainActivity
import io.github.dostepnykampus.app.debug.DebugRole
import io.github.dostepnykampus.core.location.DemoMode
import io.github.dostepnykampus.core.location.TrybPokazu
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.testing.DeviceSteps
import io.github.dostepnykampus.core.testing.openedCard
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import kotlin.math.cos
import kotlin.math.hypot

private const val WAIT_MS = 30_000L
private const val MEASURE_WAIT_MS = 120_000L
private const val STAIRS = "Schody przy wejściu"
private const val DESCRIPTION_HINT = "Obowiązkowy przy typie „inna bariera”."
private const val METERS_PER_DEGREE = 111_320.0
private const val NEAR_ENTRANCE_METERS = 10.0

/**
 * Droga demo w budynku A-1 na prawdziwym serwerze, jeden test od pomiaru do eksportu: każdy odcinek scenariusza
 * jest osobnym krokiem tej klasy, a krok zaczyna się od stanu zostawionego przez poprzedni. Kroki w aplikacji idą
 * przez ekrany jak u osoby, założenia scenariusza i eksport przez API serwera ([DemoServer]). Pozycję podaje trasa
 * pokazu, a zdjęcie aparat podstawiony intencją ([DemoDevice.stubCamera]).
 *
 * TODO(#3062): test czeka na `GET /export.geojson` na serwerze; po jego scaleniu zdjąć `@Ignore`.
 *
 * Wymagania: US6.1, US6.2, US7.1, US9.2, US10.1, US2.1, US11.1, US13.1
 */
@RunWith(AndroidJUnit4::class)
class DemoPathTest {
    /** Prawdziwa aktywność aplikacji na czystych danych. */
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val steps = DeviceSteps(compose, WAIT_MS)
    private val measuring = DeviceSteps(compose, MEASURE_WAIT_MS)
    private val device = DemoDevice(compose, steps)
    private val server = DemoServer()
    private val note = "Droga demo ${UUID.randomUUID()}"

    /** Trasa pokazu w C1, zgoda na lokalizację i aparat z intencji; zamyka instrukcję pierwszego uruchomienia. */
    @OptIn(TrybPokazu::class)
    @Before
    fun prepare() {
        DemoMode.setEnabled(InstrumentationRegistry.getInstrumentation().targetContext, isEnabled = true)
        device.grantLocation()
        Intents.init()
        device.stubCamera()
        steps.leaveHelp()
    }

    /** Przywraca sieć emulatora także po porażce testu i zwalnia podstawione intencje. */
    @After
    fun restore() {
        device.network(isOn = true)
        Intents.release()
    }

    /**
     * Cała droga TB-1 od pomiaru bez sieci do eksportu, krok po kroku.
     *
     * Scenariusz: scenariusz-demo.feature: Schody przy wejściu głównym A-1 od pomiaru do eksportu
     *
     * Wymagania: US6.1, US6.2, US7.1, US9.2, US10.1, US2.1, US11.1, US13.1
     */
    @Ignore("Serwer nie ma jeszcze GET /export.geojson (#3062); powrót testu w #4162")
    @Test
    fun demoPathInA1() {
        server.prepare()
        saveOffline()
        sendAfterNetwork()
        acceptWithSimilar()
        visitorCard()
        coordinatorProgress()
        exportGeoJson()
    }

    private fun saveOffline() {
        device.signIn(DebugRole.COLLECTOR)
        device.openWork("Zmierz i zgłoś barierę")
        measuring.waitFor(hasText("Pomiar zakończony"))
        steps.waitFor(hasText("Czy to budynek $DEMO_BUILDING?"))
        device.reveal(hasText("Potwierdź")).performClick()
        compose.onNode(hasText("Dalej")).performClick()
        steps.waitFor(hasText("Opisz barierę"))
        device.reveal(hasText("Parter")).performClick()
        device.reveal(hasText("Wybierz typ bariery")).performClick()
        steps.waitFor(hasText(STAIRS))
        compose.onAllNodes(hasText(STAIRS)).onFirst().performClick()
        device.reveal(hasText("Blokada")).performClick()
        device.reveal(hasSetTextAction() and hasAnySibling(hasText(DESCRIPTION_HINT))).performTextInput(note)
        device.network(isOn = false)
        device.reveal(hasText("Zrób zdjęcie")).performClick()
        steps.waitFor(hasText("Zrób nowe zdjęcie"))
        compose.onNode(hasText("Zapisz")).performClick()
        steps.waitFor(hasText("W telefonie"))
    }

    private fun sendAfterNetwork() {
        device.network(isOn = true)
        measuring.waitFor(hasText("Wysłane"))
        val local = hasText("W telefonie")
        compose.waitUntil(MEASURE_WAIT_MS) { compose.onAllNodes(local).fetchSemanticsNodes().isEmpty() }
    }

    private fun acceptWithSimilar() {
        device.signIn(DebugRole.VERIFIER)
        device.openWork("Zgłoszenia do sprawdzenia")
        val ours = hasText(STAIRS) and hasText("Zgłoszenie z pomiaru")
        steps.waitFor(ours)
        device.reveal(hasText("Weź do sprawdzenia") and hasAnySibling(ours)).performClick()
        steps.waitFor(hasText("Podobne zgłoszenia do 10 m"))
        val threshold = hasText("Próg")
        val fiveMeters =
            listOf("4 m", "5 m", "6 m")
                .map { meters -> hasText(meters, substring = true) }
                .reduce(SemanticsMatcher::or)
        device.reveal(hasAnySibling(threshold) and fiveMeters)
        device.reveal(hasText("Przyjmij")).performClick()
        compose.waitUntil(WAIT_MS) { server.accepted(note).isNotEmpty() }
    }

    private fun visitorCard() {
        device.signIn(DebugRole.VISITOR)
        steps.openCard(DEMO_BUILDING)
        compose.onNode(openedCard()).performClick()
        device.reveal(hasText("Wejście główne") and hasText("Niedostępne"))
        device.reveal(hasText("Wejście boczne") and hasText("Dostępne"))
        device.reveal(hasText(STAIRS, substring = true, ignoreCase = true))
    }

    private fun coordinatorProgress() {
        device.signIn(DebugRole.COORDINATOR)
        device.openWork("Postęp inwentaryzacji")
        steps.waitFor(hasText("Postęp inwentaryzacji"))
        device.reveal(hasText("4 z 5 pięter, 80%"))
    }

    private fun exportGeoJson() {
        val id = server.accepted(note).single().id
        val file = server.geoJson()
        assertEquals("Typ pliku eksportu", "FeatureCollection", file.getString("type"))
        assertFalse("Eksport bez własnego układu współrzędnych", file.has("crs"))
        val features = file.getJSONArray("features")
        val feature =
            (0 until features.length())
                .map { features.getJSONObject(it) }
                .first { it.getJSONObject("properties").getString("submissionId") == id }
        val point = feature.getJSONObject("geometry").getJSONArray("coordinates")
        val place = GeoPoint(longitude = point.getDouble(0), latitude = point.getDouble(1))
        val meters = distance(from = place, to = MAIN_ENTRANCE)
        assertTrue("TB-1 $meters m od wejścia głównego", meters < NEAR_ENTRANCE_METERS)
    }

    private fun distance(from: GeoPoint, to: GeoPoint): Double {
        val north = (from.latitude - to.latitude) * METERS_PER_DEGREE
        val east = (from.longitude - to.longitude) * METERS_PER_DEGREE * cos(Math.toRadians(to.latitude))
        return hypot(x = north, y = east)
    }
}
