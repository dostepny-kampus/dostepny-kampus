/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.measure

import android.Manifest
import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.location.ElapsedClock
import io.github.dostepnykampus.core.location.GnssSystem
import io.github.dostepnykampus.core.location.LocationSwitch
import io.github.dostepnykampus.core.location.PositionFix
import io.github.dostepnykampus.core.location.PositionSource
import io.github.dostepnykampus.core.location.Satellite
import io.github.dostepnykampus.core.location.SatelliteSource
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.capture.location.BuildingLookup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Ekran C1 z modelem widoku na odczytach testowych w czasie wirtualnym: od otwarcia nowego zgłoszenia do końca
 * pomiaru.
 *
 * Wymagania: US6.1, NFR-1, NFR-2
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class MeasurePathTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val dispatcher = UnconfinedTestDispatcher()
    private val fixes = MutableStateFlow<PositionFix?>(null)
    private val sky =
        listOf(
            satellite(GnssSystem.GPS, id = 1),
            satellite(GnssSystem.GPS, id = 2),
            satellite(GnssSystem.GALILEO, id = 3),
            satellite(GnssSystem.GLONASS, id = 4),
        )
    private val viewModel by lazy {
        MeasureViewModel(
            positions = PositionSource { fixes.filterNotNull() },
            satellites = SatelliteSource { MutableStateFlow(sky) },
            clock = ElapsedClock { dispatcher.scheduler.currentTime },
            locationSwitch = LocationSwitch { MutableStateFlow(true) },
            buildingLookup = BuildingLookup(BuildingCatalogRepository(dispatcher) { CAMPUS }, dispatcher),
        )
    }

    @Before
    fun main() {
        Dispatchers.setMain(dispatcher)
        shadowOf(ApplicationProvider.getApplicationContext<Application>())
            .grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    @After
    fun reset() {
        Dispatchers.resetMain()
    }

    /**
     * Od pierwszego odczytu 30 s odliczania z dokładnością i satelitami, potem „Pomiar zakończony”.
     * Scenariusz: pomiar-precyzyjny.feature: Uśrednianie pozycji trwa 30 s i pokazuje dane o satelitach
     */
    @Test
    fun us6_1_averaging_30_s_shown() {
        compose.setContent {
            DostepnyKampusTheme { MeasureScreen(onBack = {}, onNext = {}, viewModel = viewModel) }
        }
        compose.waitForIdle()
        fixes.value = PositionFix(POINT, ACCURACY, dispatcher.scheduler.currentTime)

        compose.onNodeWithText("Pomiar: zostało 30 s").assertExists()
        compose.onNodeWithText("Dokładność: 0,8 m (szacunek)").assertExists()
        compose.onNodeWithText("Satelity: GPS 2, Galileo 1, GLONASS 1").assertExists()

        advance(ALMOST_ALL_MS)
        compose.onNodeWithText("Pomiar: zostało 1 s").assertExists()
        compose.onNodeWithText("Pomiar zakończony").assertDoesNotExist()

        advance(LAST_SECOND_MS)
        compose.onNodeWithText("Pomiar zakończony").assertExists()
        compose.onNodeWithText("Dokładność: 0,8 m (szacunek)").assertExists()
        compose.onNodeWithText("Satelity: GPS 2, Galileo 1, GLONASS 1").assertExists()
    }

    private fun advance(millis: Long) {
        dispatcher.scheduler.advanceTimeBy(millis)
        dispatcher.scheduler.runCurrent()
        compose.waitForIdle()
    }

    private fun satellite(system: GnssSystem, id: Int): Satellite =
        Satellite(system = system, id = id, azimuthDegrees = 0.0, elevationDegrees = 45.0, isUsedInFix = true)

    private companion object {
        const val ACCURACY = 0.8
        const val ALMOST_ALL_MS = 29_000L
        const val LAST_SECOND_MS = 1_000L
        val POINT = GeoPoint(longitude = 17.06, latitude = 51.11)
        const val CAMPUS = """{"type":"FeatureCollection","attribution":"© OpenStreetMap","features":[]}"""
    }
}
