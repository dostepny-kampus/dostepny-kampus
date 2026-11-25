/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.report.form

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.capture.photo.PhotoCamera
import io.github.dostepnykampus.feature.capture.photo.PhotoPaths
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Komunikat ostrzegawczy „Ważne” przed zdjęciem na V4, C2, C6 i C7. Wymagania: NFR-16, NFR-21, NFR-25 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class ImportantNoticeTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * Wiersz: przed słowem „Ważne” stoi koło ze znakiem „!”, ozdobne dla TalkBack, bo znaczenie niesie słowo.
     *
     * Wymagania: NFR-21, NFR-18
     */
    @Test
    fun drawsSignBeforeWord() {
        compose.setContent {
            DostepnyKampusTheme { PhotoSection(photos = PhotoPaths(emptyList()), camera = PhotoCamera(onTake = {})) }
        }
        val sign = compose.onNodeWithTag(IMPORTANT_SIGN_TAG, useUnmergedTree = true).fetchSemanticsNode()
        val word = compose.onNodeWithText("Ważne").fetchSemanticsNode()
        val description = sign.config.getOrElseNullable(SemanticsProperties.ContentDescription) { null }
        assertNull("znak bez własnej nazwy dla TalkBack", description)
        assertTrue("znak przed słowem", sign.boundsInRoot.right <= word.boundsInRoot.left)
    }
}
