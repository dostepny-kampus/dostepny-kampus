/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.conflict

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.data.submission.conflict.ConflictField
import io.github.dostepnykampus.core.data.submission.conflict.VersionConflict
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.submission.Submission
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionFloor
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import io.github.dostepnykampus.core.model.submission.SubmissionStatus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant

/**
 * C4 przy konflikcie wersji: obie wersje, pole różnicy i trzy wybory.
 *
 * Wymagania: US9.3, BR-10, PW-12
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class VersionConflictScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val events = mutableListOf<String>()

    /**
     * Scenariusz: synchronizacja.feature: Przy konflikcie wersji aplikacja pokazuje obie wersje.
     */
    @Test
    fun showsBothVersions() {
        show(SubmissionStatus.SENT)
        compose.onNodeWithText("Dwie wersje: wybierz jedną.").assertIsDisplayed()
        compose.onNodeWithText("Druga wersja zostanie usunięta.").assertIsDisplayed()
        compose.onNodeWithText("Różni się: uciążliwość").assertIsDisplayed()
        compose.onNodeWithText("Moja wersja").assertIsDisplayed()
        compose.onNodeWithText("Tylko z pomocą").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Wersja z serwera").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Blokada").performScrollTo().assertIsDisplayed()
    }

    /** Przy zgłoszeniu przyjętym napis mówi, co stanie się z wersją własną. Wymagania: PW-12 */
    @Test
    fun acceptedHasSecondLine() {
        show(SubmissionStatus.ACCEPTED)
        compose.onNodeWithText(ACCEPTED_LINE).assertIsDisplayed()
    }

    /** Trzy wybory wywołują swoje czynności. */
    @Test
    fun choicesCallActions() {
        show(SubmissionStatus.SENT)
        compose.onNodeWithText("Zostaw moją wersję").performScrollTo().performClick()
        compose.onNodeWithText("Weź wersję z serwera").performScrollTo().performClick()
        compose.onNodeWithText("Zdecyduję później").performScrollTo().performClick()
        assertEquals("kolejność wyborów", listOf("moja", "serwer", "później"), events)
    }

    /** Zrzut do porównania z makietą `m-c4-synchronizacja-konflikt-jasny`. */
    @Test
    fun drawsLightConflict() {
        show(SubmissionStatus.SENT)
        compose.onRoot().captureRoboImage()
    }

    /** Zrzut do porównania z makietą `m-c4-synchronizacja-konflikt-ciemny`. */
    @Test
    @Config(qualifiers = "+night")
    fun drawsDarkConflict() {
        show(SubmissionStatus.SENT)
        compose.onRoot().captureRoboImage()
    }

    /** Zrzut przy czcionce 200 %: wersje jedna pod drugą, nic nie jest ucięte. Wymagania: NFR-20 */
    @Test
    fun drawsConflictAtFont200() {
        RuntimeEnvironment.setFontScale(2f)
        show(SubmissionStatus.SENT)
        compose.onRoot().captureRoboImage()
    }

    private fun show(status: SubmissionStatus) {
        val actions =
            ConflictActions(
                onBack = { events += "wstecz" },
                onKeepMine = { events += "moja" },
                onTakeServer = { events += "serwer" },
                onLater = { events += "później" },
            )
        compose.setContent {
            DostepnyKampusTheme {
                VersionConflictContent(state = conflictOf(status).toUiState(), actions = actions)
            }
        }
    }

    private companion object {
        const val ACCEPTED_LINE =
            "Przyjęte zgłoszenie zostanie, a Twoja wersja, jeśli ją wybierzesz, trafi do sprawdzenia jako nowe."

        fun conflictOf(status: SubmissionStatus): VersionConflict {
            val own =
                SubmissionContent(
                    kind = SubmissionKind.PRECISE,
                    location = GeoPoint(longitude = 17.06209, latitude = 51.10785),
                    photoIds = listOf("p1"),
                    buildingSymbol = "C-13",
                    floor = SubmissionFloor(number = 1),
                    barrierType = "TB-5",
                    severity = 2,
                )
            val server =
                Submission(
                    id = "s-c4",
                    version = 7,
                    status = status,
                    createdAt = Instant.parse("2026-11-20T10:00:00Z"),
                    content = own.copy(severity = 3),
                )
            return VersionConflict(id = "s-c4", own = own, server = server, differences = listOf(ConflictField.SEVERITY))
        }
    }
}
