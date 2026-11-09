/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
@file:OptIn(TymczasoweRole::class)

package io.github.dostepnykampus.app.debug

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.dostepnykampus.app.AppShell
import io.github.dostepnykampus.core.data.account.TymczasoweRole
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.Account
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Przełącznik ról wersji debug: wybór roli zmienia pasek dolny bez ponownego uruchomienia (US14.1, BR-1). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class DebugRolesTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val roles = DebugRoles()

    // Wiersz: pasek dolny, rola pracownicza → „Konto i praca”, Gość → „Konto”, US14.1.
    @Test
    fun us14_1_debug_role_changes_bar() {
        compose.setContent {
            DostepnyKampusTheme {
                AppShell(firstLaunch = false, roles = roles, onFirstHelpLeft = {}) { Text("Ekran") }
            }
        }
        compose.onNodeWithText("Konto").assertIsDisplayed()

        roles.account = DebugRole.COLLECTOR.account
        compose.onNodeWithText("Konto i praca").assertIsDisplayed()

        roles.account = DebugRole.GUEST.account
        compose.onNodeWithText("Konto").assertIsDisplayed()
    }

    @Test
    fun `przełącznik daje cztery role`() {
        val chosen = mutableListOf<DebugRole>()
        compose.setContent { DostepnyKampusTheme { RoleSwitch(current = Account.Guest, onChoose = { chosen += it }) } }
        listOf("Gość", "Odwiedzający", "Zbierający").forEach { compose.onNodeWithText(it).assertIsDisplayed() }
        compose.onNodeWithText("Weryfikator").performClick()
        assertEquals(listOf(DebugRole.VERIFIER), chosen)
    }
}
