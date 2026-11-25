/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.review.check

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.designsystem.IMPORTANT_SIGN_TAG
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceDistance
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.core.model.review.SimilarSubmission
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.core.model.submission.SubmissionField
import io.github.dostepnykampus.core.model.submission.SubmissionFloor
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import io.github.dostepnykampus.core.model.submission.SubmissionStatus
import io.github.dostepnykampus.feature.review.queue.queued
import io.github.dostepnykampus.feature.review.queue.quick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * R2 „Sprawdzenie zgłoszenia”: dane zgłoszenia z miejscem słowami, podobne zgłoszenia, decyzja, typ i uciążliwość
 * zgłoszenia szybkiego, komunikaty, poprzednie zgłoszenie przy „bariera usunięta”, zmiana oznaczenia wejścia
 * bez „Duplikat”, wczytywanie i stany wspólne.
 *
 * Wymagania: US10, US10.1, US10.2, US10.3, US10.6, NFR-19, US10.8, BR-3, BR-11, BR-19, BR-24, NFR-50
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class ReviewCheckScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var accepts = 0
    private var retries = 0
    private val severities = mutableListOf<Severity>()
    private val shown = mutableStateOf<CheckUiState>(CheckUiState.Loading)

    @Test
    fun showsSubmissionData() {
        show(READY)
        listOf("Sprawdzenie zgłoszenia", "Zgłoszenie z pomiaru", "Tylko z pomocą", "Budynek C-13").forEach { text ->
            compose.onNodeWithText(text).assertIsDisplayed()
        }
        compose.onNodeWithText("Przyjmij").performScrollTo().performClick()
        assertEquals("przyjęcie", 1, accepts)
    }

    @Test
    fun showsPlaceInWords() {
        val entrance =
            Entrance(
                id = "n/1",
                buildingSymbol = "C-13",
                name = "Wejście A",
                isMain = false,
                type = null,
                location = CHECKED.content.location,
                marking = EntranceMarking.ACCESSIBLE,
            )
        val measured = CHECKED.copy(content = CHECKED.content.copy(accuracyMeters = 0.8))
        show(READY.copy(submission = measured, nearest = EntranceDistance(entrance = entrance, meters = 2.0)))
        compose.onNodeWithText("Miejsce: ok. 2 m od wejścia „Wejście A”, dokładność 0,8 m").assertIsDisplayed()
    }

    @Test
    fun showsSimilar() {
        show(READY)
        compose.onNodeWithText("Podobne zgłoszenia do 10 m").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("4 m, parter").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Wysłane").performScrollTo().assertIsDisplayed()
        shown.value = READY.copy(similar = emptyList())
        compose.onNodeWithText("W promieniu 10 m nie ma podobnych zgłoszeń.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun showsChangedAndOffline() {
        val changed = listOf(SubmissionField.SEVERITY, SubmissionField.DESCRIPTION)
        show(READY.copy(notice = CheckNotice.CHANGED, changed = changed))
        compose.onNodeWithText("Autor zmienił zgłoszenie.").assertIsDisplayed()
        compose.onNodeWithText("Sprawdź nową wersję przed decyzją.").assertIsDisplayed()
        compose.onNodeWithText("Zmienione: uciążliwość, opis").assertIsDisplayed()
        shown.value = READY.copy(notice = CheckNotice.OFFLINE)
        compose.onNodeWithText("Bez internetu").assertIsDisplayed()
    }

    @Test
    fun quickAsksTypeAndSeverity() {
        show(READY.copy(submission = quick(CHECKED.id), notice = CheckNotice.TYPE_MISSING))
        listOf("Czego dotyczy:", "Typ bariery", "Uciążliwość").forEach { text ->
            compose.onNodeWithText(text).performScrollTo().assertIsDisplayed()
        }
        compose.onNodeWithText("Ustal typ bariery i uciążliwość, aby przyjąć zgłoszenie.").performScrollTo()
        compose.onNodeWithText("Blokada").performScrollTo().performClick()
        assertEquals("wybrana uciążliwość", listOf(Severity.BLOCKING), severities)
    }

    @Test
    fun quickMissingTypeLeadsToField() {
        val quick = READY.copy(submission = quick(CHECKED.id))
        show(quick)
        compose.onNodeWithText("Przyjmij").performScrollTo()
        shown.value = quick.copy(notice = CheckNotice.TYPE_MISSING, missingAttempts = 1)
        compose.onNodeWithText("Typ bariery").assertIsDisplayed()
        val signs = compose.onAllNodesWithTag(IMPORTANT_SIGN_TAG, useUnmergedTree = true).fetchSemanticsNodes()
        assertEquals("znak „!” przy błędzie przyjęcia", 1, signs.size)
        compose.onNodeWithText("Przyjmij").performScrollTo()
        shown.value = quick.copy(notice = CheckNotice.TYPE_MISSING, missingAttempts = 2)
        compose.onNodeWithText("Typ bariery").assertIsDisplayed()
    }

    @Test
    fun entranceHasNoDuplicate() {
        val marking = CHECKED.copy(content = CHECKED.content.copy(kind = SubmissionKind.ENTRANCE_MARKING))
        show(READY.copy(submission = marking))
        compose.onNodeWithText("Nieaktualne").performScrollTo()
        assertEquals("bez „Duplikat”", 0, compose.onAllNodes(hasTextExactly("Duplikat")).fetchSemanticsNodes().size)
    }

    @Test
    fun showsPreviousOfRemoved() {
        val removed = CHECKED.run { copy(content = content.copy(isBarrierRemoved = true, replacesSubmissionId = "p")) }
        show(
            READY.copy(
                submission = removed,
                photoUrls = listOf("a/p-a"),
                previous = PreviousSubmission(status = SubmissionStatus.ARCHIVED, photoUrl = "p/p-p"),
            ),
        )
        compose.onNodeWithText("Zdjęcie z poprzedniego zgłoszenia").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Nowe zdjęcie miejsca").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Poprzednie zgłoszenie:").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Archiwalne").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun loadsAndRetries() {
        show(CheckUiState.Loading)
        compose.onNodeWithText("Wczytujemy zgłoszenie…").assertIsDisplayed()
        shown.value = CheckUiState.Offline
        compose.onNodeWithText("Spróbuj ponownie").performClick()
        assertEquals("ponowienie", 1, retries)
    }

    private fun show(state: CheckUiState) {
        shown.value = state
        val decision = DecisionActions(onAccept = { accepts++ }, onReject = {}, onSeverity = { severities += it })
        compose.setContent {
            DostepnyKampusTheme {
                ReviewCheckContent(
                    state = shown.value,
                    actions = ReviewCheckActions(onBack = {}, onDecided = {}),
                    decision = decision,
                    onRetry = { retries++ },
                )
            }
        }
    }

    private companion object {
        val CHECKED = queued(id = "a", day = 27)
        val NEAR = queued(id = "b", day = 20).run { copy(content = content.copy(floor = SubmissionFloor(number = 0))) }
        val READY =
            CheckUiState.Ready(
                submission = CHECKED,
                photoUrls = emptyList(),
                similar = listOf(SimilarSubmission(submission = NEAR, distanceMeters = 4.0)),
            )
    }
}
