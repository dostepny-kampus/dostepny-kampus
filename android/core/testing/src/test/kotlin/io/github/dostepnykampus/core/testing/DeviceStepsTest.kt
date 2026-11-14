/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.testing

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Kroki na ekranie z pomocą, polem symbolu i wynikiem wyszukiwania, który otwiera kartę. */
@RunWith(RobolectricTestRunner::class)
class DeviceStepsTest {
    /** Aktywność bez treści; treść ustawia każdy test. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val steps = DeviceSteps(compose, waitMs = 2_000L)

    /** Wstecz zamyka pomoc, a wpisany symbol i wybór wyniku prowadzą do karty z przyciskiem wejść. */
    @Test
    fun helpThenCard() {
        compose.setContent { Campus() }
        steps.leaveHelp()
        steps.openCard("A-1")
        assertTrue("Pomoc po Wstecz", compose.onAllNodes(hasText("Pomoc")).fetchSemanticsNodes().isEmpty())
    }

    /** Węzeł, którego nie ma, kończy czekanie błędem po czasie. */
    @Test
    fun missingNodeTimesOut() {
        compose.setContent { BasicText("Mapa") }
        assertThrows(ComposeTimeoutException::class.java) { steps.waitFor(hasText(CHECK_ENTRANCES)) }
    }

    @Composable
    private fun Campus() {
        var isHelp by remember { mutableStateOf(true) }
        var query by remember { mutableStateOf("") }
        var isCard by remember { mutableStateOf(false) }
        BackHandler(enabled = isHelp) { isHelp = false }
        Column {
            if (isHelp) BasicText("Pomoc")
            BasicTextField(value = query, onValueChange = { query = it })
            if (query == "A-1") {
                BasicText(
                    text = "A-1",
                    modifier =
                        Modifier
                            .semantics { contentDescription = "A-1, Budynek główny" }
                            .clickable { isCard = true },
                )
            }
            if (isCard) BasicText(CHECK_ENTRANCES)
        }
    }
}
