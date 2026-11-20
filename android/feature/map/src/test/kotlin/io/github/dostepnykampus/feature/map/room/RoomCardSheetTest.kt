/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.room

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.map.room.RoomCardFixtures.FOUND
import io.github.dostepnykampus.feature.map.room.RoomCardFixtures.VIA_LIFT
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Treść otwartej karty sali i powód braku drogi. Wymagania: US18.1, US18.2, US18.3 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-xhdpi")
class RoomCardSheetTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun showsAllStepsAndOpensBuilding() {
        val opened = mutableListOf<String>()
        show(VIA_LIFT, onBuilding = { opened.add(it) })

        compose.onNode(hasText("Sala 329") and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)).assertExists()
        compose.onNodeWithText("Jak dojść").assertExists()
        compose.onNodeWithText("Ok. 30 m od wejścia.", substring = true).assertExists()
        compose.onNodeWithText("ok. 85 m od windy.", substring = true).assertExists()
        compose.onNodeWithContentDescription("Budynek A-1").performClick()
        assertEquals("Otwarto inny budynek", listOf("A-1"), opened)
    }

    @Test
    fun explainsMissingLift() {
        show(RoomWay.NoLift)

        compose.onNodeWithText("Żadna winda nie łączy", substring = true).assertExists()
    }

    @Test
    fun largeTextCanScrollToMapAction() {
        var opens = 0
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 2f)) {
                DostepnyKampusTheme {
                    RoomCardSheet(
                        card = RoomCard(found = FOUND, way = VIA_LIFT),
                        onClose = {},
                        onBuilding = {},
                        onShowMap = { opens += 1 },
                    )
                }
            }
        }
        compose.onNodeWithText("Pokaż na mapie").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("Przejście do planu", 1, opens) }
    }

    @Test
    fun matchesLightMockup() {
        show(VIA_LIFT)
        compose.onRoot().captureRoboImage()
    }

    private fun show(way: RoomWay, onBuilding: (String) -> Unit = {}) {
        compose.setContent {
            DostepnyKampusTheme {
                RoomCardSheet(
                    card = RoomCard(found = FOUND, way = way),
                    onClose = {},
                    onBuilding = onBuilding,
                )
            }
        }
    }
}
