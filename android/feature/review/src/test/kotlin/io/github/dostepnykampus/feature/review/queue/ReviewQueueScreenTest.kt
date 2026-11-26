/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.review.queue

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.submission.SubmissionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * R1 „Zgłoszenia do sprawdzenia”: pozycje kolejki, „Wzięte przeze mnie” z „Otwórz”, „Weź do sprawdzenia”,
 * pusta kolejka, wczytywanie, zgłoszenie zajęte, brak sieci i brak uprawnień.
 *
 * Wymagania: US10.4, US10.7, BR-11, NFR-50
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class ReviewQueueScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val opened = mutableListOf<String>()
    private val claimed = mutableListOf<String>()
    private var retries = 0

    @Test
    fun showsQueueItem() {
        show(ReviewQueueUiState.Ready(waiting = listOf(WAITING), mine = emptyList()))
        listOf("Do sprawdzenia", "Próg", "Budynek C-13", "Zgłoszenie z pomiaru", "Wysłane 27.10.2026").forEach { text ->
            compose.onNodeWithText(text).assertIsDisplayed()
        }
        compose.onNodeWithText("Weź do sprawdzenia").performClick()
        assertEquals("wzięcie zgłoszenia z kolejki", listOf(WAITING.id), claimed)
    }

    @Test
    fun opensMine() {
        show(ReviewQueueUiState.Ready(waiting = listOf(WAITING), mine = listOf(MINE)))
        compose.onNodeWithText("Wzięte przeze mnie").assertIsDisplayed()
        compose.onNodeWithText("Otwórz").performClick()
        assertEquals("otwarcie wziętego zgłoszenia", listOf(MINE.id), opened)
    }

    /** „Weź do sprawdzenia” i „Otwórz” to czynności karty szerokie jak napis, węższe niż treść karty. */
    @Test
    fun keepsCardActionsCompact() {
        show(ReviewQueueUiState.Ready(waiting = listOf(WAITING), mine = listOf(MINE)))
        listOf("Weź do sprawdzenia", "Otwórz").forEach { label ->
            val bounds = compose.onNodeWithText(label).getUnclippedBoundsInRoot()
            assertTrue("„$label” ma być węższe niż treść karty: $bounds", (bounds.right - bounds.left).value < CONTENT)
        }
    }

    @Test
    fun showsEmptyQueue() {
        show(ReviewQueueUiState.Ready(waiting = emptyList(), mine = emptyList()))
        compose.onNodeWithText("Nie ma zgłoszeń do sprawdzenia. Zajrzyj tu później.").assertIsDisplayed()
    }

    @Test
    fun showsTakenNotice() {
        show(ReviewQueueUiState.Ready(waiting = listOf(WAITING), mine = emptyList(), isTaken = true))
        compose.onNodeWithText("Ktoś inny wziął już to zgłoszenie.").assertIsDisplayed()
        compose.onNodeWithText("Wybierz inne.").assertIsDisplayed()
    }

    @Test
    fun showsLoading() {
        show(ReviewQueueUiState.Loading)
        compose.onNodeWithText("Wczytujemy zgłoszenia…").assertIsDisplayed()
    }

    @Test
    fun retriesWhenOffline() {
        show(ReviewQueueUiState.Offline)
        compose.onNodeWithText("Spróbuj ponownie").performClick()
        assertEquals("ponowny odczyt", 1, retries)
    }

    @Test
    fun forbiddenLeadsBack() {
        var backs = 0
        show(ReviewQueueUiState.Forbidden, onBack = { backs++ })
        compose.onNodeWithText("Wróć do mapy").performClick()
        assertEquals("powrót bez uprawnień", 1, backs)
    }

    private fun show(state: ReviewQueueUiState, onBack: () -> Unit = {}) {
        compose.setContent {
            DostepnyKampusTheme {
                ReviewQueueContent(
                    state = state,
                    onBack = onBack,
                    onOpen = { opened += it },
                    onClaim = { claimed += it },
                    onRetry = { retries++ },
                )
            }
        }
    }

    private companion object {
        const val CONTENT = 296f

        val WAITING = queued(id = "a", day = 27)
        val MINE = queued(id = "b", day = 26).copy(status = SubmissionStatus.IN_REVIEW)
    }
}
