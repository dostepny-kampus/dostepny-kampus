/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.github.dostepnykampus.core.data.account.GuestRoleProvider
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.help.HelpScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Ekran V5 „Pomoc” w Robolectric: kroki, obrazy, „Wstecz” w obu wariantach, duża czcionka i oboczka. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class HelpScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val calls = mutableListOf<String>()

    private fun show(isFirstLaunch: Boolean = false) {
        compose.setContent {
            DostepnyKampusTheme { HelpScreen(isFirstLaunch, { calls += "mapa" }, onLeave = { calls += "wyjście" }) }
        }
    }

    private fun shell(firstLaunch: Boolean) {
        compose.setContent {
            DostepnyKampusTheme {
                AppShell(firstLaunch, GuestRoleProvider, onFirstHelpLeft = { calls += "pokazana" })
            }
        }
    }

    // Scenariusz: przegladanie-mapy.feature: Instrukcja przy pierwszym uruchomieniu
    @Test
    fun us5_1_first_launch_skip_v1() {
        shell(firstLaunch = true)
        compose.onNodeWithText("Jak korzystać z\u00A0aplikacji").assertExists()
        compose.onNodeWithText("Krok 1 z 3").assertExists()
        compose.onNodeWithText("Pomiń instrukcję").performClick()
        compose.onNode(MapTab).assertIsSelected()
        assertEquals(listOf("pokazana"), calls)
    }

    // Scenariusz: przegladanie-mapy.feature: Instrukcję można otworzyć ponownie z pomocy
    @Test
    fun us5_1_help_opens_again() {
        shell(firstLaunch = false)
        compose.onNode(MapTab).assertIsSelected()
        compose.onAllNodesWithText("Jak korzystać z aplikacji").assertCountEquals(0)
        compose.onNodeWithText("Pomoc").performClick()
        compose.onNodeWithText("Znajdź budynek").assertExists()
        compose.onNodeWithText("Dalej").performClick()
        compose.onNodeWithText("Pomoc").performClick()
        compose.onNodeWithText("Krok 1 z 3").assertExists()
    }

    // Wiersz: „Wstecz”: poprzedni krok; w pierwszym kroku przy pierwszym uruchomieniu jak „Pomiń”, US5.1.
    @Test
    fun us5_1_first_launch_wstecz() {
        show(isFirstLaunch = true)
        compose.onNodeWithText("Dalej").performClick()
        compose.onNodeWithText("Wstecz").performClick()
        compose.onNodeWithText("Krok 1 z 3").assertExists()
        compose.onNodeWithText("Wstecz").performClick()
        compose.onNodeWithText("Pomiń instrukcję").performClick()
        assertEquals(listOf("mapa", "mapa"), calls)
    }

    // Wiersz: „Dalej”, a w ostatnim kroku „Zacznij” → następny krok albo V1, numer kroku, US5.1, NFR-21;
    // systemowe „Wstecz” cofa krok, a w kroku 1 po otwarciu z paska wraca do poprzedniego ekranu, NFR-15.
    @Test
    fun us5_1_bar_dalej_back_zacznij() {
        show()
        compose.onNodeWithText("Dalej").performClick()
        compose.onNodeWithText("Co znaczy »Dostępne«").assertExists()
        compose.waitForIdle()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("Krok 1 z 3").assertExists()
        compose.onNodeWithText("Wstecz").performClick()
        repeat(times = 2) { compose.onNodeWithText("Dalej").performClick() }
        compose.onNodeWithText("Krok 3 z 3").assertExists()
        compose.onNodeWithText("Zgłoś barierę").assertExists()
        compose.onAllNodesWithText("Dalej").assertCountEquals(0)
        compose.onNodeWithText("Zacznij").performClick()
        assertEquals(listOf("wyjście", "mapa"), calls)
    }

    // Wiersz: obraz pokazuje prawdziwy element aplikacji i ma opis dla TalkBack, US5.1, NFR-18.
    @Test
    fun nfr18_example_talkback() {
        show()
        compose.onNodeWithContentDescription("Przykład z mapy kampusu", substring = true).assertExists()
        compose.onAllNodesWithText("Wpisz symbol budynku, np. C-13").assertCountEquals(0)
        compose.onNodeWithText("Dalej").performClick()
        compose.onNodeWithContentDescription("Przykład z opisu budynku A-1", substring = true).assertExists()
        compose.onAllNodesWithText("Wejście dostosowane").assertCountEquals(0)
    }

    // Wiersz: reguła 10 dużej czcionki, czynności V5 na końcu treści, po tekście kroku, NFR-20, NFR-52.
    @Test
    fun nfr20_actions_after_text() {
        RuntimeEnvironment.setFontScale(2f)
        show()
        val text = compose.onNodeWithText("Wpisz symbol budynku", substring = true).getUnclippedBoundsInRoot()
        val next = compose.onNodeWithText("Dalej").performScrollTo().getUnclippedBoundsInRoot()
        val skip = compose.onNodeWithText("Pomiń instrukcję").getUnclippedBoundsInRoot()
        assertTrue(next.top > text.bottom && skip.top > next.bottom)
    }
}
