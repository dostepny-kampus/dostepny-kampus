/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.dostepnykampus.core.data.submissions.RefusalReason
import io.github.dostepnykampus.core.data.submissions.SendState
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.BarrierType
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class MySubmissionsScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val opened = mutableListOf<String>()
    private var measured = 0

    private fun show(vararg items: OwnSubmissionItem) {
        showState(MySubmissionsUiState.Ready(MySubmissions(items.toList())))
    }

    private fun showState(state: MySubmissionsUiState, onOpen: ((String) -> Unit)? = { opened.add(it) }) {
        val actions = MySubmissionsActions(onBack = {}, onMeasure = { measured++ }, onOpen = onOpen)
        compose.setContent {
            DostepnyKampusTheme {
                MySubmissionsContent(state = state, actions = actions)
            }
        }
    }

    /** Wiersz: C3 pusty → „Nie masz jeszcze zgłoszeń…” i „Zmierz i zgłoś barierę” do C1, PW-12. */
    @Test
    fun pw12_empty_list() {
        show()
        compose.onNodeWithText("Nie masz jeszcze zgłoszeń. Zmierz i zgłoś pierwszą barierę.").assertIsDisplayed()
        compose.onNodeWithText("Zmierz i zgłoś barierę").performClick()
        assertEquals(1, measured)
    }

    /**
     * Wiersz: C3 → „Czeka na wysłanie: N” przy zgłoszeniach w telefonie bez „Nie wysłano”, US9.1, BR-26.
     * Scenariusz: synchronizacja.feature: Licznik oczekujących nie liczy zgłoszeń „Nie wysłano”
     */
    @Test
    fun us9_1_waiting_count() {
        show(
            item(id = "a", state = SendState.LOCAL),
            item(id = "b", state = SendState.LOCAL),
            item(id = "c", state = SendState.REFUSED),
        )
        compose.onNodeWithText("Czeka na wysłanie: 2").assertIsDisplayed()
    }

    /** Wiersz: C3 ładowanie → nagłówek bez listy i bez stanu pustego. */
    @Test
    fun us8_1_loading() {
        showState(MySubmissionsUiState.Loading)
        compose.onNodeWithText("Moje zgłoszenia").assertIsDisplayed()
        compose.onNodeWithText("Zmierz i zgłoś barierę").assertDoesNotExist()
    }

    /** Wiersz: C3 same wysłane → bez „Czeka na wysłanie”, US9.1. */
    @Test
    fun us9_1_nothing_waiting() {
        show(item(id = "a", state = SendState.SENT))
        compose.onNodeWithText("Wysłane").assertIsDisplayed()
        compose.onNodeWithText("Czeka na wysłanie", substring = true).assertDoesNotExist()
    }

    /** Wiersz: C3, poprawka w C2 jeszcze niedostępna → pozycje bez „Otwórz”, US8.3. */
    @Test
    fun us8_3_list_without_open() {
        val list = MySubmissions(listOf(item(id = "a", state = SendState.LOCAL)))
        showState(MySubmissionsUiState.Ready(list), onOpen = null)
        compose.onNodeWithText("W telefonie").assertIsDisplayed()
        compose.onNodeWithText("Otwórz").assertDoesNotExist()
    }

    /** Wiersz: C3 „Otwórz” w pozycji → poprawka w C2 z identyfikatorem zgłoszenia, US8.3. */
    @Test
    fun us8_3_open_item() {
        show(item(id = "a", state = SendState.LOCAL))
        compose.onNodeWithText("Otwórz").performClick()
        assertEquals(listOf("a"), opened)
    }

    /**
     * Wiersz: C3 „Nie wysłano” → powód i „Otwórz” do poprawki w C2, bez liczenia w „Czeka na wysłanie”, US9.5.
     * Scenariusz (częściowo): synchronizacja.feature: Odrzucona treść albo zdjęcie
     *   zostawia zgłoszenie ze statusem „Nie wysłano”
     */
    @Test
    fun us9_5_refused_open() {
        val refused = item(id = "a", state = SendState.REFUSED).copy(refusal = RefusalReason.NOT_ACCEPTED)
        show(refused)
        compose.onNodeWithText("Nie wysłano").assertIsDisplayed()
        val reason = "Serwer nie przyjął zgłoszenia. Otwórz je, popraw i zapisz ponownie."
        compose.onNodeWithText(reason).assertIsDisplayed()
        compose.onNodeWithText("Czeka na wysłanie", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Otwórz").performClick()
        assertEquals(listOf("a"), opened)
    }

    /** Wiersz: C3 → „Sprawdzamy statusy…”, przy błędzie komunikat i „Spróbuj ponownie”, US8.2, NFR-50, PW-10. */
    @Test
    fun us8_2_checking_and_retry() {
        var retried = 0
        var check by mutableStateOf(StatusCheck.CHECKING)
        compose.setContent {
            DostepnyKampusTheme {
                val actions = MySubmissionsActions(onBack = {}, onMeasure = {})
                val state = MySubmissionsUiState.Ready(MySubmissions(emptyList()), check)
                MySubmissionsContent(state = state, actions = actions, onRetry = { retried++ })
            }
        }
        compose.onNodeWithText("Sprawdzamy statusy…").assertIsDisplayed()
        check = StatusCheck.FAILED
        compose.onNodeWithText("Nie udało się sprawdzić statusów", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Spróbuj ponownie").performClick()
        assertEquals(1, retried)
        check = StatusCheck.DONE
        compose.onNodeWithText("Spróbuj ponownie").assertDoesNotExist()
    }

    /** Wiersz: C3 „Wysłane” → bez „Otwórz”, bo treść jest tylko na serwerze, US8.3. */
    @Test
    fun us8_3_sent_without_open() {
        show(item(id = "a", state = SendState.SENT), item(id = "b", state = SendState.REFUSED))
        compose.onNodeWithText("Otwórz").performClick()
        assertEquals(listOf("b"), opened)
    }
}

private fun item(id: String, state: SendState) =
    OwnSubmissionItem(
        id = id,
        savedAt = Instant.parse("2026-11-09T12:00:00Z"),
        type = BarrierType.STAIRS_AT_ENTRANCE,
        buildingSymbol = "A-1",
        floorNumber = 2,
        state = state,
        refusal = null,
    )
