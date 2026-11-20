/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.room

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotEnabled
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
import io.github.dostepnykampus.feature.map.barrier.ShownBarriers
import io.github.dostepnykampus.feature.map.entrance.EntranceActions
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

        val heading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)
        compose.onNode(hasText("Sala 329") and heading).assertExists()
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
        show(VIA_LIFT, density = Density(density = 1f, fontScale = 2f), onShowMap = { opens += 1 })
        compose.onNodeWithText("Pokaż na mapie").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("Przejście do planu", 1, opens) }
    }

    @Test
    fun matchesLightMockup() {
        show(VIA_LIFT)
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun walksStepsAndReturnsFromLift() {
        compose.setContent {
            DostepnyKampusTheme {
                var card by remember { mutableStateOf(RoomCard(found = FOUND, way = VIA_LIFT)) }
                RoomCardLayer(
                    room = OpenRoom(card = card, onClose = {}, onStep = { card = card.chosen(it) }),
                    sources = RoomSheetSources(onBuilding = {}, ShownBarriers(emptyList()), EntranceActions()),
                )
            }
        }

        compose.onNodeWithText("Pokaż po kolei").performClick()
        compose.onNodeWithText("Krok 1 z 3").assertExists()
        compose.onNodeWithContentDescription("Poprzedni krok").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Następny krok").performClick()
        compose.onNodeWithText("Krok 2 z 3").assertExists()
        compose.onNodeWithText("Szczegóły windy").performClick()
        compose.onNodeWithText("Jedzie na:", substring = true).assertExists()
        compose.onNodeWithText("Zamknij").performClick()

        compose.onNodeWithText("Krok 2 z 3").assertExists()
        compose.onNodeWithText("ok. 85 m od windy.", substring = true).performClick()
        compose.onNodeWithText("Krok 3 z 3").assertExists()
        compose.onNodeWithContentDescription("Następny krok").assertIsNotEnabled()
    }

    private fun show(
        way: RoomWay,
        onBuilding: (String) -> Unit = {},
        density: Density? = null,
        onShowMap: (() -> Unit)? = null,
    ) {
        val room = OpenRoom(card = RoomCard(found = FOUND, way = way), onClose = {}, onStep = {})
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides (density ?: LocalDensity.current)) {
                DostepnyKampusTheme {
                    RoomCardSheet(room = room, onBuilding = onBuilding, onShowMap = onShowMap)
                }
            }
        }
    }
}
