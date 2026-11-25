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
 * R2 „Sprawdzenie zgłoszenia”: dane zgłoszenia, „Przyjmij”, powody odrzucenia z „Duplikat” i „Inny powód”,
 * komunikaty nowej wersji, braku powodu i braku sieci oraz zmiana oznaczenia wejścia bez „Duplikat”.
 *
 * Wymagania: US10.2, US10.8, BR-11, BR-13, BR-16, BR-24
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class ReviewCheckScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val rejections = mutableListOf<Rejection>()
    private var accepts = 0
    private val shown = mutableStateOf<CheckUiState>(CheckUiState.Loading)

    @Test
    fun showsSubmissionData() {
        show(READY)
        listOf("Zgłoszenie z pomiaru", "Tylko z pomocą", "Budynek C-13").forEach { text ->
            compose.onNodeWithText(text).assertIsDisplayed()
        }
        click("Przyjmij")
        assertEquals("przyjęcie", 1, accepts)
    }

    @Test
    fun rejectsDuplicateOfEarlier() {
        show(READY)
        click("Duplikat")
        click("Próg, 4 m")
        click("Odrzuć")
        assertEquals("duplikat ze wskazaniem", listOf(Rejection(reason = "Duplikat", duplicateOf = "b")), rejections)
    }

    @Test
    fun showsNotices() {
        show(READY.copy(notice = CheckNotice.REASON_MISSING))
        compose.onNodeWithText("Podaj powód odrzucenia, aby autor wiedział, co poprawić.").performScrollTo()
        shown.value = READY.copy(notice = CheckNotice.CHANGED)
        compose.onNodeWithText("Autor zmienił zgłoszenie.").assertIsDisplayed()
        shown.value = READY.copy(notice = CheckNotice.OFFLINE)
        compose.onNodeWithText("Bez internetu").assertIsDisplayed()
        compose.onNodeWithText("Połącz się z internetem i podejmij decyzję ponownie.").performScrollTo()
    }

    @Test
    fun entranceHasNoDuplicate() {
        val marking = CHECKED.copy(content = CHECKED.content.copy(kind = SubmissionKind.ENTRANCE_MARKING))
        show(READY.copy(submission = marking))
        compose.onNodeWithText("Nieaktualne").performScrollTo()
        assertEquals("bez „Duplikat”", 0, compose.onAllNodes(hasTextExactly("Duplikat")).fetchSemanticsNodes().size)
    }

    private fun click(text: String) {
        compose.onNodeWithText(text).performScrollTo().performClick()
    }

    private fun show(state: CheckUiState) {
        shown.value = state
        compose.setContent {
            DostepnyKampusTheme {
                ReviewCheckContent(
                    state = shown.value,
                    actions = ReviewCheckActions(onBack = {}, onDecided = {}),
                    decision = DecisionActions(onAccept = { accepts++ }, onReject = { rejections += it }),
                    onRetry = {},
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
