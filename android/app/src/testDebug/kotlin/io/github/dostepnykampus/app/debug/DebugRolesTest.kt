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

/** Przełącznik ról wersji debug: wybór roli zmienia pasek dolny bez ponownego uruchomienia. Wymagania: US14.1, BR-1 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class DebugRolesTest {
    /** Ekran testowy Compose. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val roles = DebugRoles()

    /** Pasek dolny: rola pracownicza daje „Konto i praca”, Gość „Konto”. Wymagania: US14.1 */
    @Test
    fun debugRoleChangesBar() {
        compose.setContent {
            DostepnyKampusTheme {
                AppShell(firstLaunch = false, roles = roles, onFirstHelpLeft = {}) { Text("Ekran") }
            }
        }
        compose.onNodeWithText("Konto").assertIsDisplayed()

        roles.switchTo(DebugRole.COLLECTOR.account)
        compose.onNodeWithText("Konto i praca").assertIsDisplayed()

        roles.switchTo(DebugRole.GUEST.account)
        compose.onNodeWithText("Konto").assertIsDisplayed()
    }

    /** Przełącznik pokazuje cztery role i zwraca wybraną. */
    @Test
    fun switchOffersFourRoles() {
        val chosen = mutableListOf<DebugRole>()
        compose.setContent { DostepnyKampusTheme { RoleSwitch(current = Account.Guest, onChoose = { chosen += it }) } }
        listOf("Gość", "Odwiedzający", "Zbierający").forEach { compose.onNodeWithText(it).assertIsDisplayed() }
        compose.onNodeWithText("Weryfikator").performClick()
        assertEquals(listOf(DebugRole.VERIFIER), chosen)
    }
}
