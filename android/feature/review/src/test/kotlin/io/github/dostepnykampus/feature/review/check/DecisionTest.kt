/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.review.check

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.review.SimilarSubmission
import io.github.dostepnykampus.feature.review.queue.queued
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Decyzja R2: „Przyjmij”, powody odrzucenia z „Duplikat” i „Inny powód”, brak powodu, komunikaty braku powodu
 * i braku sieci oraz zmiana oznaczenia wejścia bez „Duplikat”.
 *
 * Wymagania: US10.2, BR-11, BR-13, BR-16, BR-24
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class DecisionTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val rejections = mutableListOf<Rejection>()
    private var accepts = 0

    @Test
    fun acceptsAndRejectsOutdated() {
        show()
        click("Przyjmij")
        click("Nieaktualne")
        click("Odrzuć")
        assertEquals("przyjęcie", 1, accepts)
        assertEquals("powód gotowy", listOf(Rejection(reason = "Nieaktualne")), rejections)
    }

    @Test
    fun rejectsDuplicateOfEarlier() {
        show()
        click("Duplikat")
        compose.onNodeWithText("Wcześniejsze zgłoszenie:").assertIsDisplayed()
        click("Próg, 4 m")
        click("Odrzuć")
        assertEquals("duplikat ze wskazaniem", listOf(Rejection(reason = "Duplikat", duplicateOf = "b")), rejections)
    }

    @Test
    fun rejectsWithOwnReason() {
        show()
        click("Inny powód")
        compose.onNodeWithText("Opisz powód dla autora").performScrollTo().performTextInput("Zdjęcie nieostre")
        click("Odrzuć")
        assertEquals("własny powód", listOf(Rejection(reason = "Zdjęcie nieostre")), rejections)
    }

    @Test
    fun rejectsWithoutReason() {
        show(notice = CheckNotice.REASON_MISSING)
        click("Odrzuć")
        assertEquals("bez wybranego powodu", listOf(Rejection(reason = "")), rejections)
        compose.onNodeWithText("Podaj powód odrzucenia, aby autor wiedział, co poprawić.").performScrollTo()
    }

    @Test
    fun showsOfflineNotice() {
        show(notice = CheckNotice.OFFLINE)
        val message = "Decyzja wymaga internetu, a zgłoszenie czeka w »Wziętych przeze mnie«."
        compose.onNodeWithText(message).assertIsDisplayed()
        compose.onNodeWithText("Połącz się z internetem i podejmij decyzję ponownie.").assertIsDisplayed()
    }

    @Test
    fun entranceHasNoDuplicate() {
        show(allowsDuplicate = false)
        compose.onNodeWithText("Nieaktualne").assertIsDisplayed()
        assertEquals("bez „Duplikat”", 0, compose.onAllNodes(hasTextExactly("Duplikat")).fetchSemanticsNodes().size)
    }

    private fun click(text: String) {
        compose.onNodeWithText(text).performScrollTo().performClick()
    }

    private fun show(notice: CheckNotice? = null, allowsDuplicate: Boolean = true) {
        compose.setContent {
            DostepnyKampusTheme {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Decision(
                        similar = SIMILAR,
                        allowsDuplicate = allowsDuplicate,
                        notice = notice,
                        actions = DecisionActions(onAccept = { accepts++ }, onReject = { rejections += it }),
                    )
                }
            }
        }
    }

    private companion object {
        val SIMILAR = listOf(SimilarSubmission(submission = queued(id = "b", day = 20), distanceMeters = 4.0))
    }
}
