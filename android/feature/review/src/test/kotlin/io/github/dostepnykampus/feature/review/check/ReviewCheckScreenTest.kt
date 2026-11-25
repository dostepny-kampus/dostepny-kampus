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
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.review.SimilarSubmission
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import io.github.dostepnykampus.feature.review.queue.queued
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * R2 „Sprawdzenie zgłoszenia”: dane zgłoszenia z decyzją, komunikaty nowej wersji i braku sieci,
 * zmiana oznaczenia wejścia bez „Duplikat”, wczytywanie i stany wspólne.
 *
 * Wymagania: US10, US10.2, US10.8, BR-3, BR-11, BR-24, NFR-50
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class ReviewCheckScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var accepts = 0
    private var retries = 0
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
    fun showsChangedAndOffline() {
        show(READY.copy(notice = CheckNotice.CHANGED))
        compose.onNodeWithText("Autor zmienił zgłoszenie.").assertIsDisplayed()
        shown.value = READY.copy(notice = CheckNotice.OFFLINE)
        compose.onNodeWithText("Bez internetu").assertIsDisplayed()
    }

    @Test
    fun entranceHasNoDuplicate() {
        val marking = CHECKED.copy(content = CHECKED.content.copy(kind = SubmissionKind.ENTRANCE_MARKING))
        show(READY.copy(submission = marking))
        compose.onNodeWithText("Nieaktualne").performScrollTo()
        assertEquals("bez „Duplikat”", 0, compose.onAllNodes(hasTextExactly("Duplikat")).fetchSemanticsNodes().size)
    }

    @Test
    fun showsLoading() {
        show(CheckUiState.Loading)
        compose.onNodeWithText("Wczytujemy zgłoszenie…").assertIsDisplayed()
    }

    @Test
    fun retriesAfterOffline() {
        show(CheckUiState.Offline)
        compose.onNodeWithText("Spróbuj ponownie").performClick()
        assertEquals("ponowienie", 1, retries)
    }

    private fun show(state: CheckUiState) {
        shown.value = state
        compose.setContent {
            DostepnyKampusTheme {
                ReviewCheckContent(
                    state = shown.value,
                    actions = ReviewCheckActions(onBack = {}, onDecided = {}),
                    decision = DecisionActions(onAccept = { accepts++ }, onReject = {}),
                    onRetry = { retries++ },
                )
            }
        }
    }

    private companion object {
        val CHECKED = queued(id = "a", day = 27)
        val READY =
            CheckUiState.Ready(
                submission = CHECKED,
                photoUrls = emptyList(),
                similar = listOf(SimilarSubmission(submission = queued(id = "b", day = 20), distanceMeters = 4.0)),
            )
    }
}
