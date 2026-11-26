/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Znak „!” w kole przed ostrzeżeniem i błędem pola. Wymagania: NFR-21 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class ImportantSignTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** Znak jest widoczny i nie ma własnego opisu, bo znaczenie niesie tekst obok. Wymagania: NFR-21, NFR-18 */
    @Test
    fun signHasNoDescription() {
        compose.setContent {
            DostepnyKampusTheme { ImportantSign(tint = MaterialTheme.colorScheme.error) }
        }
        val sign = compose.onNodeWithTag(IMPORTANT_SIGN_TAG, useUnmergedTree = true)
        sign.assertIsDisplayed()
        val config = sign.fetchSemanticsNode().config
        val description = config.getOrElseNullable(SemanticsProperties.ContentDescription) { null }
        assertNull("znak bez własnej nazwy dla TalkBack", description)
    }
}
