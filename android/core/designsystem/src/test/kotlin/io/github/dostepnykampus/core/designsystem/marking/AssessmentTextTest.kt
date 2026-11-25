/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem.marking

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.building.AssessmentSource
import io.github.dostepnykampus.core.model.building.EntranceAssessment
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Dzień i źródło oznaczenia wejścia słowami (PW-3). Wymagania: US17.3, BR-24, BR-27 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class AssessmentTextTest {
    @get:Rule
    val compose = createComposeRule()

    /** Każde źródło ma własne słowa i dzień w zapisie kampusu. Wymagania: US17.3, BR-24, BR-27 */
    @Test
    fun namesEverySource() {
        val day = LocalDate.of(2026, 11, 3)
        compose.setContent {
            DostepnyKampusTheme {
                Column {
                    AssessmentSource.entries.forEach { AssessmentLine(EntranceAssessment(date = day, source = it)) }
                }
            }
        }
        compose.onNodeWithText("Z map i opisów, stan z 3.11.2026").assertExists()
        compose.onNodeWithText("Z planów Politechniki Wrocławskiej, stan z 3.11.2026").assertExists()
        compose.onNodeWithText("Sprawdzone na miejscu 3.11.2026").assertExists()
    }
}
