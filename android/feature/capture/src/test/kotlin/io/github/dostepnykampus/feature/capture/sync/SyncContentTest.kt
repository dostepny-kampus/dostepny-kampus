/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.sync

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.github.dostepnykampus.core.data.submission.sync.SyncProgress
import io.github.dostepnykampus.core.data.submission.sync.SyncStatus
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.designsystem.IMPORTANT_SIGN_TAG
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/** Widok C4 bez konfliktu: liczba, podział, ostatnia synchronizacja i stany. Wymagania: US9.1, US9.2, US9.4, NFR-7 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class SyncContentTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val calls = mutableListOf<String>()

    private fun show(phase: SyncPhase, status: SyncStatus = QUEUE, isLowSpace: Boolean = false) {
        val actions = SyncActions(onBack = {}, onSyncNow = { calls += "teraz" }, onLogin = { calls += "zaloguj" })
        val state = SyncUiState.Ready(status = status, phase = phase, isLowSpace = isLowSpace)
        compose.setContent { DostepnyKampusTheme { SyncContent(state = state, actions = actions) } }
    }

    /** Wiersz: kolejka z podziałem i chwilą ostatniej synchronizacji; „Synchronizuj teraz” rusza wysyłkę. */
    @Test
    fun showsQueue() {
        show(SyncPhase.Waiting)

        compose.onNodeWithText("Czeka na wysłanie: 3").assertIsDisplayed()
        compose.onNodeWithText("2 zgłoszenia i 1 znacznik piętra").assertIsDisplayed()
        compose.onNodeWithText("Ostatnia synchronizacja: 12.11.2026, 10:42").assertIsDisplayed()
        compose.onNodeWithText("Ważne").assertDoesNotExist()
        compose.onNodeWithText("Synchronizuj teraz").performScrollTo().performClick()
        assertEquals("„Synchronizuj teraz” rusza wysyłkę", listOf("teraz"), calls)
    }

    /** Wiersz: pusta kolejka to jedno zdanie z datą ostatniej synchronizacji, bez licznika. Wymagania: US9.1 */
    @Test
    fun showsAllSent() {
        show(SyncPhase.Done, status = QUEUE.copy(submissions = 0, markers = 0))

        compose.onNodeWithText("Wszystko wysłane. Ostatnia synchronizacja: 12.11.2026, 10:42.").assertIsDisplayed()
        compose.onNodeWithText("Czeka na wysłanie", substring = true).assertDoesNotExist()
    }

    /** Wiersz: postęp przebiegu „Wysyłamy X z Y…”. Wymagania: US9.1 */
    @Test
    fun showsProgress() {
        show(SyncPhase.Sending(SyncProgress(sent = 2, total = 5)))

        compose.onNodeWithText("Wysyłamy 2 z 5…").assertIsDisplayed()
    }

    /** Wiersz: bez sieci zgłoszenia czekają i wyślą się same. Wymagania: US9.1, US9.2 */
    @Test
    fun explainsOffline() {
        show(SyncPhase.Offline)

        compose.onNodeWithText("Bez internetu. Wyślemy wszystko sami, gdy internet wróci.").assertIsDisplayed()
    }

    /** Wiersz: błąd serwera mówi, że zgłoszenia czekają w telefonie. Wymagania: NFR-50, US9.4 */
    @Test
    fun explainsServerDown() {
        show(SyncPhase.ServerDown)

        val server = "Serwer chwilowo nie odpowiada. Zgłoszenia czekają w telefonie i wyślą się później."
        compose.onNodeWithText(server).assertIsDisplayed()
    }

    /** Wiersz: wygasłe zalogowanie ma „Zaloguj się” zamiast „Synchronizuj teraz”. Wymagania: US9.4, BR-26 */
    @Test
    fun asksToLogIn() {
        show(SyncPhase.LoginExpired)

        compose.onNodeWithText("Logowanie wygasło. Zaloguj się, a wyślemy zgłoszenia same.").assertIsDisplayed()
        compose.onNodeWithText("Synchronizuj teraz").assertDoesNotExist()
        compose.onNodeWithText("Zaloguj się").performScrollTo().performClick()
        assertEquals("„Zaloguj się” otwiera logowanie", listOf("zaloguj"), calls)
    }

    /** Wiersz: ostrzeżenie o mniej niż 100 MB wolnego miejsca. Wymagania: NFR-7 */
    @Test
    fun warnsAboutSpace() {
        show(SyncPhase.Waiting, isLowSpace = true)

        val warning = "W telefonie zostało mniej niż 100 MB. Zwolnij miejsce, aby dalej robić zdjęcia."
        compose.onNodeWithText(warning).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Ważne").assertIsDisplayed()
        compose.onNodeWithTag(IMPORTANT_SIGN_TAG).assertIsDisplayed()
    }

    internal companion object {
        val QUEUE =
            SyncStatus(
                submissions = 2,
                markers = 1,
                lastSyncedAt = Instant.parse("2026-11-12T09:42:00Z"),
                isLastRunFailed = false,
            )
    }
}
