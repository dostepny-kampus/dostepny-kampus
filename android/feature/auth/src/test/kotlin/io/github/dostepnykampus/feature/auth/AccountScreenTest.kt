/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.auth

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.Account
import io.github.dostepnykampus.core.model.StaffRole
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** L2 „Konto”: role konta, czynności ról i wylogowanie. Wymagania: US14.1, US14.3, US14.4 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class AccountScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val events = mutableListOf<String>()

    /** Zbierający i Koordynator: wszystkie role na karcie i czynności obu ról. */
    @Test
    fun staffSeesRolesAndWork() {
        show(Account.SignedIn(setOf(StaffRole.COORDINATOR, StaffRole.COLLECTOR)))
        compose.onNodeWithText("Konto i praca").assertIsDisplayed()
        compose.onNodeWithText("Twoje role: Odwiedzający, Zbierający, Koordynator").assertIsDisplayed()
        compose.onNodeWithText("Zmierz i zgłoś barierę").performScrollTo().performClick()
        compose.onNodeWithText("Moje zgłoszenia").performScrollTo().performClick()
        compose.onNodeWithText("Postęp inwentaryzacji").performScrollTo().performClick()
        assertEquals("czynności ról", listOf("MEASURE", "MINE", "PROGRESS"), events)
    }

    /** „Wyloguj się” i „Wstecz” prowadzą poza ekran. */
    @Test
    fun signsOutAndGoesBack() {
        show(Account.SignedIn())
        compose.onNodeWithText("Wyloguj się").performScrollTo().performClick()
        compose.onNodeWithText("Wstecz").performClick()
        assertEquals("wylogowanie i powrót", listOf("signOut", "back"), events)
    }

    private fun show(account: Account.SignedIn) {
        val actions =
            AccountActions(
                onBack = { events += "back" },
                onWork = { work -> events += work.name },
                onSignOut = { events += "signOut" },
            )
        compose.setContent {
            DostepnyKampusTheme {
                AccountScreen(account = account, actions = actions)
            }
        }
    }
}
