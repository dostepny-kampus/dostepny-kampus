/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.inspection.progress

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.inspection.BuildingProgress
import io.github.dostepnykampus.core.model.inspection.InspectionCoverage
import io.github.dostepnykampus.core.model.inspection.InspectionMarker
import io.github.dostepnykampus.core.model.inspection.MarkerPlace
import io.github.dostepnykampus.core.model.inspection.ProgressMarker
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import java.util.TimeZone

/**
 * Zrzuty K1 „Postęp inwentaryzacji” z listą budynków i C5 Koordynatora ze znacznikami i pytaniem o cofnięcie.
 * Strefa czasowa na czas testu to Warszawa, więc godzina „Stan z …” jest ta sama w każdym środowisku.
 *
 * Wymagania: US11.1, US11.3, NFR-20
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class InspectionScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val zone = TimeZone.getDefault()

    @Before
    fun warsaw() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Warsaw"))
    }

    @After
    fun restore() {
        TimeZone.setDefault(zone)
    }

    private fun capture(content: @Composable () -> Unit) {
        compose.setContent { DostepnyKampusTheme { content() } }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage()
    }

    private fun progress() =
        capture {
            ProgressContent(
                state = ProgressUiState.Ready(buildings = listOf(FLOORS, WHOLE), readAt = READ_AT),
                actions = ProgressActions(onBack = {}, onOpen = {}),
                onRefresh = {},
            )
        }

    private fun markers(asking: String?) =
        capture {
            MarkersContent(
                state = MarkersUiState.Ready(building = FLOORS, asking = asking),
                actions = MarkerActions(onAsk = {}, onKeep = {}, onConfirm = {}, onRetry = {}),
                onBack = {},
            )
        }

    /** K1: budynek z piętrami i budynek oglądany w całości. Wymagania: US11.1 */
    @Test
    fun drawsLightProgress() {
        progress()
    }

    @Test
    @Config(qualifiers = "+night")
    fun drawsDarkProgress() {
        progress()
    }

    /** K1 przy czcionce 200 %: karty rosną w dół, nic nie jest ucięte. Wymagania: NFR-20 */
    @Test
    fun drawsProgressAtFont200() {
        RuntimeEnvironment.setFontScale(2f)
        progress()
    }

    /** C5: znacznik piętra z autorem i „Cofnij znacznik”. Wymagania: US11.3 */
    @Test
    fun drawsLightMarkers() {
        markers(asking = null)
    }

    @Test
    @Config(qualifiers = "+night")
    fun drawsDarkMarkers() {
        markers(asking = null)
    }

    /** C5: pytanie o potwierdzenie cofnięcia. Wymagania: US11.3, BR-20 */
    @Test
    fun drawsLightAsking() {
        markers(asking = MARKER_ID)
    }

    private companion object {
        const val MARKER_ID = "7f0c1c1e-0000-4000-8000-000000000001"
        val READ_AT: Instant = Instant.parse("2026-11-15T09:30:00Z")
        val FLOORS =
            BuildingProgress(
                buildingSymbol = "A-1",
                coverage = InspectionCoverage.Floors(floorsTotal = 5, floorsInspected = 4),
                pendingSubmissions = 3,
                acceptedSubmissions = 4,
                entrancesTotal = 6,
                entrancesCheckedOnSite = 1,
                markers =
                    listOf(
                        ProgressMarker(
                            marker =
                                InspectionMarker(
                                    id = MARKER_ID,
                                    buildingSymbol = "A-1",
                                    place = MarkerPlace.Floor(floorId = "2"),
                                    createdAt = Instant.parse("2026-11-12T10:00:00Z"),
                                    isRevoked = false,
                                    isMine = false,
                                ),
                            authorPseudonym = "Autor 14",
                        ),
                    ),
            )
        val WHOLE =
            BuildingProgress(
                buildingSymbol = "B-4",
                coverage = InspectionCoverage.Whole(isInspected = true),
                pendingSubmissions = 0,
                acceptedSubmissions = 2,
                entrancesTotal = 2,
                entrancesCheckedOnSite = 2,
                markers = emptyList(),
            )
    }
}
