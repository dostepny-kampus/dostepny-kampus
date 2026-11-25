/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.inspection.invitation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.StaffRole
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Zrzuty K2 do porównania z makietami hi-fi `m-k2-*`.
 *
 * Wymagania: US12.1, US12.2, NFR-20
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class InvitationScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** K2 z błędami adresu i roli, motyw jasny. */
    @Test
    fun drawsLightProblems() {
        draw(PROBLEMS)
    }

    /** K2 z błędami adresu i roli, motyw ciemny. */
    @Test
    @Config(qualifiers = "+night")
    fun drawsDarkProblems() {
        draw(PROBLEMS)
    }

    /** K2 po dodaniu roli przy czcionce 200 %: nic nie jest ucięte. */
    @Test
    fun drawsRoleAddedAtFont200() {
        RuntimeEnvironment.setFontScale(2f)
        draw(InvitationUiState(stage = InvitationStage.ROLE_ADDED, addedRole = StaffRole.VERIFIER))
    }

    private fun draw(state: InvitationUiState) {
        compose.setContent {
            DostepnyKampusTheme {
                InvitationContent(
                    state = state,
                    onBack = {},
                    onLeave = {},
                    onEdit = {},
                    onChoose = {},
                    onSend = {},
                )
            }
        }
        compose.onRoot().captureRoboImage()
    }

    private companion object {
        val PROBLEMS =
            InvitationUiState(
                email = "anna.nowak.pwr.edu.pl",
                problems = setOf(InvitationProblem.EMAIL, InvitationProblem.ROLE),
            )
    }
}
