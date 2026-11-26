/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.mine

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.github.dostepnykampus.core.data.submission.own.RefusalReason
import io.github.dostepnykampus.core.data.submission.own.SendState
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.core.model.submission.BarrierType
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import io.github.dostepnykampus.core.model.submission.SubmissionStatus
import io.github.dostepnykampus.feature.capture.navigation.MarkingRoute
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
    fun showsEmptyList() {
        show()
        compose.onNodeWithText("Nie masz jeszcze zgłoszeń. Zmierz i zgłoś pierwszą barierę.").assertIsDisplayed()
        compose.onNodeWithText("Zmierz i zgłoś barierę").performClick()
        assertEquals(1, measured)
    }

    /**
     * Wskaźnik sieci znika po połączeniu, a zgłoszenie nadal jest w telefonie; zmianę odczytuje TalkBack.
     * Scenariusz (częściowo): synchronizacja.feature: Bez sieci widać wskaźnik,
     *   a zgłoszenie czeka ze statusem „lokalne”
     */
    @Test
    fun showsOfflineIndicator() {
        var isOnline by mutableStateOf(false)
        val list = MySubmissions(listOf(item(id = "a", state = SendState.LOCAL)))
        compose.setContent {
            DostepnyKampusTheme {
                MySubmissionsContent(
                    state = MySubmissionsUiState.Ready(list, isOnline = isOnline),
                    actions = MySubmissionsActions(onBack = {}, onMeasure = {}),
                )
            }
        }
        compose.onNodeWithText("Bez internetu").assertIsDisplayed()
        compose.onNodeWithText("W telefonie").assertIsDisplayed()
        val polite = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)
        compose.onNode(polite and hasAnyDescendant(hasText("Bez internetu"))).assertExists()

        isOnline = true
        compose.onNodeWithText("Bez internetu").assertDoesNotExist()
        compose.onNodeWithText("W telefonie").assertIsDisplayed()
    }

    /**
     * Wiersz: C3 → „Czeka na wysłanie: N” przy zgłoszeniach w telefonie bez „Nie wysłano”.
     * Scenariusz: synchronizacja.feature: Licznik oczekujących nie liczy zgłoszeń „Nie wysłano”
     *
     * Wymagania: US9.1, BR-26
     */
    @Test
    fun showsWaitingCount() {
        show(
            item(id = "a", state = SendState.LOCAL),
            item(id = "b", state = SendState.LOCAL),
            item(id = "c", state = SendState.REFUSED),
        )
        compose.onNodeWithText("Czeka na wysłanie: 2").assertIsDisplayed()
    }

    /** Wiersz: C3 ładowanie → nagłówek bez listy i bez stanu pustego. */
    @Test
    fun showsLoading() {
        showState(MySubmissionsUiState.Loading)
        compose.onNodeWithText("Moje zgłoszenia").assertIsDisplayed()
        compose.onNodeWithText("Zmierz i zgłoś barierę").assertDoesNotExist()
    }

    /** Wiersz: C3 same wysłane → bez „Czeka na wysłanie”. Wymagania: US9.1 */
    @Test
    fun hidesWaitingWhenAllSent() {
        show(item(id = "a", state = SendState.SENT))
        compose.onNodeWithText("Wysłane").assertIsDisplayed()
        compose.onNodeWithText("Czeka na wysłanie", substring = true).assertDoesNotExist()
    }

    /** Wiersz: C3, poprawka w C2 jeszcze niedostępna → pozycje bez „Otwórz”. Wymagania: US8.3 */
    @Test
    fun listsWithoutOpen() {
        val list = MySubmissions(listOf(item(id = "a", state = SendState.LOCAL)))
        showState(MySubmissionsUiState.Ready(list), onOpen = null)
        compose.onNodeWithText("W telefonie").assertIsDisplayed()
        compose.onNodeWithText("Otwórz").assertDoesNotExist()
    }

    /** Wiersz: C3 „Otwórz” w pozycji → poprawka w C2 z identyfikatorem zgłoszenia. Wymagania: US8.3 */
    @Test
    fun opensItem() {
        show(item(id = "a", state = SendState.LOCAL))
        compose.onNodeWithText("Otwórz").performClick()
        assertEquals(listOf("a"), opened)
    }

    /**
     * Wiersz: C3 „Nie wysłano” → powód i „Otwórz” do poprawki w C2, bez liczenia w „Czeka na wysłanie”.
     * Scenariusz (częściowo): synchronizacja.feature: Odrzucona treść albo zdjęcie
     *   zostawia zgłoszenie ze statusem „Nie wysłano”
     *
     * Wymagania: US9.5
     */
    @Test
    fun opensRefusedItem() {
        val refused = item(id = "a", state = SendState.REFUSED).copy(refusal = RefusalReason.NOT_ACCEPTED)
        show(refused)
        compose.onNodeWithText("Nie wysłano").assertIsDisplayed()
        val reason = "Serwer nie przyjął zgłoszenia. Otwórz je, popraw i zapisz ponownie."
        compose.onNodeWithText(reason).assertIsDisplayed()
        compose.onNodeWithText("Czeka na wysłanie", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Otwórz").performClick()
        assertEquals(listOf("a"), opened)
    }

    /**
     * Wiersz: C3 → „Sprawdzamy statusy…”, przy błędzie komunikat i „Spróbuj ponownie”, PW-10.
     *
     * Wymagania: US8.2, NFR-50
     */
    @Test
    fun showsCheckingAndRetry() {
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

    /**
     * Wiersz: C3 „Przyjęte” i „Archiwalne” → bez „Otwórz”; „Wysłane” z „Otwórz”.
     *
     * Wymagania: US8.1, US8.3
     */
    @Test
    fun opensOnlyEditable() {
        show(
            item(id = "a", state = SendState.SENT).copy(serverStatus = SubmissionStatus.ACCEPTED),
            item(id = "b", state = SendState.SENT).copy(serverStatus = SubmissionStatus.ARCHIVED),
            item(id = "c", state = SendState.SENT).copy(serverStatus = SubmissionStatus.SENT),
        )
        compose.onNodeWithText("Przyjęte").assertIsDisplayed()
        compose.onNodeWithText("Archiwalne").assertIsDisplayed()
        compose.onNodeWithText("Otwórz").performClick()
        assertEquals(listOf("c"), opened)
    }

    /**
     * Wiersz: C3 znak „Dwie wersje: wybierz jedną” → C4 zgłoszenia z konfliktem wersji.
     *
     * Wymagania: US9.3
     */
    @Test
    fun conflictOpensChoice() {
        val conflicted = item(id = "k", state = SendState.REFUSED).copy(refusal = RefusalReason.CHANGED_ON_SERVER)
        val chosen = mutableListOf<String>()
        val actions = MySubmissionsActions(onBack = {}, onMeasure = {}, onConflict = { chosen += it })
        val state = MySubmissionsUiState.Ready(MySubmissions(listOf(conflicted)))
        compose.setContent {
            DostepnyKampusTheme {
                MySubmissionsContent(state = state, actions = actions)
            }
        }
        compose.onNodeWithText("Dwie wersje: wybierz jedną").performClick()
        assertEquals("C4 zgłoszenia z konfliktem", listOf("k"), chosen)
    }

    /**
     * Wiersz: C3 „Otwórz” przy zmianie oznaczenia wejścia → C6 tej zmiany; „Wyślij ponownie” po „nieaktualne” → C6
     * tego wejścia z bieżącym oznaczeniem.
     *
     * Wymagania: US8.3, US17.2, BR-23
     */
    @Test
    fun opensMarkingChange() {
        val waiting = marking(id = "m-1").copy(state = SendState.LOCAL)
        val outdated =
            marking(id = "m-2").copy(serverStatus = SubmissionStatus.REJECTED, rejectionReason = "nieaktualne")
        val routes = mutableListOf<MarkingRoute>()
        val actions = MySubmissionsActions(onBack = {}, onMeasure = {}, onOpen = {}, onOpenMarking = { routes += it })
        val state = MySubmissionsUiState.Ready(MySubmissions(listOf(waiting, outdated)))
        compose.setContent {
            DostepnyKampusTheme { MySubmissionsContent(state = state, actions = actions) }
        }
        compose.onNodeWithText("Otwórz").performScrollTo().performClick()
        compose.onNodeWithText("Wyślij ponownie").performScrollTo().performClick()
        val entrance = MarkingRoute(entranceId = "n/1", buildingSymbol = "B-8")
        assertEquals("C6 poprawki, potem C6 wejścia", listOf(entrance.copy(submissionId = "m-1"), entrance), routes)
    }

    /** Przy konflikcie wersji nie ma „Otwórz”: konflikt rozstrzyga tylko wybór w C4. Wymagania: US9.3, BR-10 */
    @Test
    fun conflictHasNoOpen() {
        val conflicted = item(id = "k", state = SendState.REFUSED).copy(refusal = RefusalReason.CHANGED_ON_SERVER)
        val opened = mutableListOf<String>()
        val actions = MySubmissionsActions(onBack = {}, onMeasure = {}, onOpen = { opened += it }, onConflict = {})
        val state = MySubmissionsUiState.Ready(MySubmissions(listOf(conflicted)))
        compose.setContent {
            DostepnyKampusTheme { MySubmissionsContent(state = state, actions = actions) }
        }
        compose.onNodeWithText("Dwie wersje: wybierz jedną").assertExists()
        compose.onNodeWithText("Otwórz").assertDoesNotExist()
        assertEquals("C2 się nie otwiera", emptyList<String>(), opened)
    }

    /** Pozycja bez konfliktu wersji nie ma znaku wyboru wersji. Wymagania: US9.3 */
    @Test
    fun noConflictNoChoice() {
        show(item(id = "a", state = SendState.REFUSED).copy(refusal = RefusalReason.NO_PERMISSION))
        compose.onNodeWithText("Dwie wersje: wybierz jedną").assertDoesNotExist()
    }
}

private fun marking(id: String) =
    item(id = id, state = SendState.SENT).copy(
        kind = SubmissionKind.ENTRANCE_MARKING,
        type = null,
        buildingSymbol = "B-8",
        floorNumber = null,
        entranceId = "n/1",
        entranceMarking = EntranceMarking.ACCESSIBLE,
    )

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
