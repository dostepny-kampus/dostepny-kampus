/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.core.model.submission.BarrierType
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.core.model.submission.SubmissionFloor
import io.github.dostepnykampus.feature.map.photoAvailable
import io.github.dostepnykampus.feature.map.usePhotoLoader
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/**
 * Karta budynku podstawowego B-8 zamiast planu piętra: napis o braku planu, wejścia i zgłoszenie przyjęte.
 *
 * Scenariusz: budynki-bez-szczegolow.feature: Karta budynku podstawowego zamiast planu piętra pokazuje napis
 *
 * Wymagania: US16.2, BR-17
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class BasicBuildingCardTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun showsNoPlanInsteadOfFloors() {
        show()

        compose.onNodeWithText("Nie mamy planu pięter tego budynku.").performScrollTo().assertIsDisplayed()
        val entrances = compose.onAllNodesWithText("Główne", substring = true).fetchSemanticsNodes()
        assertTrue("wejście B-8 na karcie budynku podstawowego", entrances.isNotEmpty())
        compose.onNodeWithText("Bariery w budynku").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Blokada").performScrollTo().assertIsDisplayed()
    }

    private fun show() {
        usePhotoLoader(compose.activity, photoAvailable())
        compose.setContent {
            DostepnyKampusTheme {
                BuildingCardContent(
                    symbol = "B-8",
                    mode = CardMode.Expanded,
                    card =
                        BuildingCardUiState.Ready(
                            symbol = "B-8",
                            name = null,
                            entrances = orderedEntrances(listOf(ENTRANCE), listOf(BARRIER)),
                            barriers = listOf(BARRIER),
                            floors = null,
                            isBasic = true,
                        ),
                    isOnline = true,
                    onShowOnMap = {},
                    onBack = {},
                    onClose = {},
                    onReport = {},
                    onRetry = {},
                )
            }
        }
    }

    private companion object {
        val LOCATION = GeoPoint(longitude = 0.5, latitude = 0.5)

        val ENTRANCE =
            Entrance(
                id = "b8-main",
                buildingSymbol = "B-8",
                name = "Główne",
                isMain = true,
                type = null,
                location = LOCATION,
                marking = EntranceMarking.ACCESSIBLE,
            )

        val BARRIER =
            AcceptedBarrier(
                id = "z-b8",
                location = LOCATION,
                severity = Severity.BLOCKING,
                type = BarrierType.STAIRS_AT_ENTRANCE,
                buildingSymbol = "B-8",
                verifiedAt = Instant.parse("2026-11-12T10:00:00Z"),
                entranceId = "b8-main",
                floor = SubmissionFloor(number = 0),
                photoIds = listOf("p1"),
                photoUrl = null,
            )
    }
}
