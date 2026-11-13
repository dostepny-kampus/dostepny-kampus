/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.mine

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToString
import io.github.dostepnykampus.core.data.submissions.RefusalReason
import io.github.dostepnykampus.core.data.submissions.SendState
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.BarrierType
import io.github.dostepnykampus.core.model.SubmissionStatus
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

    private fun show(item: OwnSubmissionItem, onOpen: (() -> Unit)? = { opened++ }) {
        compose.setContent {
            DostepnyKampusTheme {
                SubmissionCard(item = item, onOpen = onOpen)
            }
        }
    }

    /** Wiersz: C3 zgłoszenie → status, typ, budynek z piętrem i data. Wymagania: US8.1 */
    @Test
    fun us8_1_item_details() {
        show(item(id = "a", state = SendState.SENT))
        compose.onNodeWithText("Wysłane").assertIsDisplayed()
        compose.onNodeWithText("Budynek A-1, Piętro 2").assertIsDisplayed()
        compose.onNodeWithText("9.11.2026").assertIsDisplayed()
    }

    /**
     * Wiersz: C3 po sprawdzeniu → status z serwera, przy odrzuconym powód; poprawka „W telefonie”.
     *
     * Wymagania: US8.2, BR-9
     */
    @Test
    fun us8_2_server_statuses() {
        val words =
            mapOf(
                SubmissionStatus.SENT to "Wysłane",
                SubmissionStatus.IN_REVIEW to "W weryfikacji",
                SubmissionStatus.ACCEPTED to "Przyjęte",
                SubmissionStatus.ARCHIVED to "Archiwalne",
            )
        val shown = words.keys.map { item(id = "a", state = SendState.SENT).copy(serverStatus = it).shownStatus() }
        assertEquals(listOf(ShownStatus.SENT, ShownStatus.IN_REVIEW, ShownStatus.ACCEPTED, ShownStatus.ARCHIVED), shown)
        val local = item(id = "a", state = SendState.LOCAL).copy(serverStatus = SubmissionStatus.ACCEPTED)
        assertEquals(ShownStatus.LOCAL, local.shownStatus())
        val rejected = item(id = "a", state = SendState.SENT).copy(serverStatus = SubmissionStatus.REJECTED)
        show(rejected.copy(rejectionReason = "Zdjęcie nieostre"))
        compose.onNodeWithText("Odrzucone").assertIsDisplayed()
        compose.onNodeWithText("Powód odrzucenia: Zdjęcie nieostre").assertIsDisplayed()
    }

    /** Wiersz: C3 „Nie wysłano” → powód odmowy. Wymagania: US9.5, BR-26 */
    @Test
    fun us9_5_refusal_reason() {
        show(item(id = "a", state = SendState.REFUSED, refusal = RefusalReason.NO_PERMISSION))
        compose.onNodeWithText("Nie wysłano").assertIsDisplayed()
        val reason = "Nie masz już uprawnień do tej czynności. Jeśli to pomyłka, poproś Koordynatora o zaproszenie."
        compose.onNodeWithText(reason).assertIsDisplayed()
    }

    /** Wiersz: C3 „Otwórz” → poprawka w C2. Wymagania: US8.3 */
    @Test
    fun us8_3_open_item() {
        show(item(id = "a", state = SendState.LOCAL))
        compose.onNodeWithText("Otwórz").performClick()
        assertEquals(1, opened)
    }

    /** Wiersz: C3 bez poprawki w C2 → pozycja bez „Otwórz”. Wymagania: US8.3 */
    @Test
    fun us8_3_no_open_without_edit() {
        show(item(id = "a", state = SendState.LOCAL), onOpen = null)
        compose.onNodeWithText("Otwórz").assertDoesNotExist()
    }

    /** Wiersz: C3 poza budynkiem, nieznany kod typu → „Poza budynkiem”, „Nieznany typ bariery”. Wymagania: US8.1 */
    @Test
    fun us8_1_outside_unknown_type() {
        show(item(id = "a", state = SendState.SENT).copy(type = null, buildingSymbol = null, floorNumber = null))
        compose.onNodeWithText("Poza budynkiem").assertIsDisplayed()
        compose.onNodeWithText("Nieznany typ bariery").assertIsDisplayed()
    }

    /** Wiersz: C3 budynek bez numeru piętra → sam budynek. Wymagania: US8.1 */
    @Test
    fun us8_1_building_without_floor() {
        show(item(id = "a", state = SendState.SENT).copy(floorNumber = null))
        compose.onNodeWithText("Budynek A-1").assertIsDisplayed()
    }

    /** Wiersz: C3 → TalkBack czyta status przed typem bariery. Wymagania: NFR-18 */
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
