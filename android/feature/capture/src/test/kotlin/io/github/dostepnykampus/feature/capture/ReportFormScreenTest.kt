/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.capture.navigation.ReportFormRoute
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** C2 w aplikacji: formularz z miejscem z C1 i wyjście „Wstecz” bez zmian. Wymagania: US7, US7.4 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class ReportFormScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var left = 0
    private var saved = 0

    private fun show() {
        val viewModel = ReportFormViewModel { _, _, _ -> }
        val transcoder = PhotoTranscoder()
        compose.setContent {
            DostepnyKampusTheme {
                ReportFormScreen(
                    route =
                        ReportFormRoute(
                            longitude = 17.06209,
                            latitude = 51.10785,
                            accuracyMeters = 0.8,
                            buildingSymbol = "A-1",
                            buildingName = "Gmach Główny",
                        ),
                    onLeave = { left++ },
                    onSave = { saved++ },
                    viewModel = viewModel,
                    photoTranscoder = transcoder,
                )
            }
        }
    }

    /** Wiersz: C1, „Dalej” → C2 z miejscem z pomiaru. Wymagania: US6, US7.4 */
    @Test
    fun us6_form_with_measured_place() {
        show()
        compose.onNodeWithText("Opisz barierę").assertIsDisplayed()
        compose.onNodeWithText("Gmach Główny", substring = true).assertIsDisplayed()
    }

    /** Wiersz: C2, „Wstecz” bez zmian → poprzedni ekran bez pytania. Wymagania: US7.4 */
    @Test
    fun us7_4_back_unchanged_leaves() {
        show()
        compose.onNodeWithText("Wstecz").performClick()
        compose.waitForIdle()
        assertEquals(1, left)
        assertEquals(0, saved)
    }
}
