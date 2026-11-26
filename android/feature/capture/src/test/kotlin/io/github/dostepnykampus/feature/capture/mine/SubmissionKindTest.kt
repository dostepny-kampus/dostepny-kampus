/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.mine

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.dostepnykampus.core.data.submission.own.SendState
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.core.model.submission.BarrierType
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import io.github.dostepnykampus.core.model.submission.SubmissionStatus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/** Pozycje C3 według rodzaju i odrzucenie „nieaktualne”. Wymagania: US8.1, US17.2, BR-23, BR-24 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class SubmissionKindTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val opened = mutableListOf<String>()
    private val removals = mutableListOf<String>()
    private val resent = mutableListOf<String>()
    private var measured = 0

    private fun show(vararg items: OwnSubmissionItem) {
        val actions =
            MySubmissionsActions(
                onBack = {},
                onMeasure = { measured++ },
                onOpen = { opened.add(it) },
                onOpenRemoval = { removals.add(it) },
                onResendChange = { resent.add(it) },
            )
        val state = MySubmissionsUiState.Ready(MySubmissions(items.toList()))
        compose.setContent { DostepnyKampusTheme { MySubmissionsContent(state = state, actions = actions) } }
    }

    /** Wiersz: zmiana oznaczenia wejścia ma nazwę rodzaju zamiast typu bariery i nie otwiera C2. Wymagania: US17.2 */
    @Test
    fun namesMarkingChange() {
        show(item("m", kind = SubmissionKind.ENTRANCE_MARKING))
        compose.onNodeWithText("Zmiana oznaczenia wejścia").assertIsDisplayed()
        compose.onNodeWithText("Nieznany typ bariery").assertDoesNotExist()
        compose.onNodeWithText("Otwórz").assertDoesNotExist()
    }

    /** Wiersz: zmiana oznaczenia wejścia pokazuje „Nowe oznaczenie:” ze słowem przy znaku. Wymagania: US17.2, NFR-21 */
    @Test
    fun showsNewMarking() {
        show(item("m", kind = SubmissionKind.ENTRANCE_MARKING).copy(entranceMarking = EntranceMarking.ACCESSIBLE))
        compose.onNodeWithText("Nowe oznaczenie:").assertIsDisplayed()
        compose.onNodeWithText("Dostępne").assertIsDisplayed()
    }

    /**
     * Wiersz: „bariera usunięta” ma „Bariery już nie ma” przed typem, a „Otwórz” prowadzi do C7, nie do C2.
     *
     * Wymagania: US8.3, US8.4, BR-19
     */
    @Test
    fun namesRemovedBarrier() {
        show(item("u", kind = SubmissionKind.PRECISE).copy(isBarrierRemoved = true))
        compose.onNodeWithText("Bariery już nie ma").assertIsDisplayed()
        compose.onNodeWithText("Schody przy wejściu").assertIsDisplayed()
        compose.onNodeWithText("Otwórz").performClick()
        assertEquals("„Otwórz” otwiera C7 tego zgłoszenia", listOf("u"), removals)
        assertEquals("C2 się nie otwiera", emptyList<String>(), opened)
    }

    /**
     * Wiersz: „bariera usunięta” odrzucona jako „nieaktualne” ma zdanie o mapie i „Zmierz i zgłoś barierę” do C1.
     *
     * Wymagania: BR-23
     */
    @Test
    fun measuresAfterOutdatedRemoval() {
        show(item("u", kind = SubmissionKind.PRECISE).copy(isBarrierRemoved = true, rejectionReason = "nieaktualne"))
        val sentence = "Bariery nie ma już na mapie. Jeśli nadal istnieje, zgłoś ją od nowa."
        compose.onNodeWithText(sentence).assertIsDisplayed()
        compose.onNodeWithText("Wyślij ponownie").assertDoesNotExist()
        val buttons = compose.onAllNodesWithText("Zmierz i zgłoś barierę")
        buttons.assertCountEquals(2)
        buttons.onLast().performClick()
        assertEquals("„Zmierz i zgłoś barierę” otwiera C1", 1, measured)
    }

    /**
     * Wiersz: zgłoszenie odrzucone jako „nieaktualne” ma zdanie o nowszej wersji i „Wyślij ponownie” do nowej zmiany
     * bieżącego przyjętego zgłoszenia zamiast poprawki tej zmiany.
     *
     * Wymagania: BR-23, BR-18, US8.3
     */
    @Test
    fun resendsOutdatedReport() {
        show(item("s", kind = SubmissionKind.PRECISE).copy(rejectionReason = "nieaktualne"))
        val sentence = "Ktoś już zmienił to zgłoszenie. Twoja zmiana trafi do jego aktualnej wersji."
        compose.onNodeWithText(sentence).assertIsDisplayed()
        compose.onNodeWithText("Otwórz").assertDoesNotExist()
        compose.onNodeWithText("Wyślij ponownie").performClick()
        assertEquals("nowa zmiana, nie poprawka", listOf(emptyList<String>(), listOf("s")), listOf(opened, resent))
    }

    /** Wiersz: zmiana oznaczenia odrzucona jako „nieaktualne” ma zdanie o zmienionym oznaczeniu. Wymagania: BR-24 */
    @Test
    fun explainsOutdatedMarking() {
        show(item("m", kind = SubmissionKind.ENTRANCE_MARKING).copy(rejectionReason = "nieaktualne"))
        val sentence = "Oznaczenie tego wejścia już się zmieniło. Sprawdź je i wyślij swoją zmianę ponownie."
        compose.onNodeWithText(sentence).assertIsDisplayed()
    }

    /** Wiersz: inny powód odrzucenia nie daje zdania „nieaktualne” i zostawia „Otwórz”. Wymagania: BR-16 */
    @Test
    fun keepsOpenForOtherReason() {
        show(item("s", kind = SubmissionKind.PRECISE).copy(rejectionReason = "zdjęcie nie pokazuje bariery"))
        compose.onNodeWithText("Ktoś już zmienił", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Otwórz").assertIsDisplayed()
    }

    private fun item(id: String, kind: SubmissionKind) =
        OwnSubmissionItem(
            id = id,
            savedAt = Instant.parse("2026-11-09T12:00:00Z"),
            type = BarrierType.STAIRS_AT_ENTRANCE.takeIf { kind == SubmissionKind.PRECISE },
            buildingSymbol = "B-8",
            floorNumber = null,
            state = SendState.SENT,
            refusal = null,
            serverStatus = SubmissionStatus.REJECTED,
            kind = kind,
        )
}
