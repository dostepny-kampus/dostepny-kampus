/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToString
import io.github.dostepnykampus.core.data.submissions.SendState
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.BarrierType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class SubmissionCardTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var opened = 0

    private fun show(item: OwnSubmissionItem) {
        compose.setContent {
            DostepnyKampusTheme {
                SubmissionCard(item = item, onOpen = { opened++ })
            }
        }
    }

    // Wiersz: C3 zgłoszenie → status, typ, budynek z piętrem i data, US8.1.
    @Test
    fun us8_1_item_details() {
        show(item(id = "a", state = SendState.SENT))
        compose.onNodeWithText("Wysłane").assertIsDisplayed()
        compose.onNodeWithText("Budynek A-1, Piętro 2").assertIsDisplayed()
        compose.onNodeWithText("9.11.2026").assertIsDisplayed()
    }

    // Wiersz: C3 „Nie wysłano” → powód odmowy, US9.5, BR-26.
    @Test
    fun us9_5_refusal_reason() {
        show(item(id = "a", state = SendState.REFUSED, refusal = RefusalReason.NO_PERMISSION))
        compose.onNodeWithText("Nie wysłano").assertIsDisplayed()
        val reason = "Nie masz już uprawnień do tej czynności. Jeśli to pomyłka, poproś Koordynatora o zaproszenie."
        compose.onNodeWithText(reason).assertIsDisplayed()
    }

    // Wiersz: C3 „Otwórz” → poprawka w C2, US8.3.
    @Test
    fun us8_3_open_item() {
        show(item(id = "a", state = SendState.LOCAL))
        compose.onNodeWithText("Otwórz").performClick()
        assertEquals(1, opened)
    }

    // Wiersz: C3 → TalkBack czyta status przed typem bariery, NFR-18.
    @Test
    fun nfr18_status_before_type() {
        show(item(id = "a", state = SendState.LOCAL))
        val tree = compose.onRoot(useUnmergedTree = false).printToString()
        assertTrue(tree.indexOf("W telefonie") in 0 until tree.indexOf("Schody przy wejściu"))
    }
}

private fun item(id: String, state: SendState, refusal: RefusalReason? = null) =
    OwnSubmissionItem(
        id = id,
        savedAt = Instant.parse("2026-11-09T12:00:00Z"),
        type = BarrierType.STAIRS_AT_ENTRANCE,
        buildingSymbol = "A-1",
        floorNumber = 2,
        state = state,
        refusal = refusal,
    )
