/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.plan

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Arkusz toalety dostosowanej i schodów po dotknięciu znaku planu. Wymagania: US16.1, NFR-18, NFR-53 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class PlanSignSheetTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var closed = 0

    /** Wiersz: toaleta dostosowana na planie ma nagłówek z fokusem, budynek z piętrem z danych i „Zamknij”. */
    @Test
    fun showsToilet() {
        show("wc")

        compose.onNodeWithText("Toaleta dostosowana").assertIsFocused()
        compose.onNodeWithText("Budynek A-1, parter").assertExists()
        compose.onNodeWithText("Zamknij").assertHeightIsAtLeast(48.dp).performClick()
        assertEquals("„Zamknij” zamyka arkusz", 1, closed)
    }

    /** Wiersz: schody na planie mają nagłówek „Schody” i piętro z danych PWr. Wymagania: US16.1 */
    @Test
    fun showsStairs() {
        show("schody")

        compose.onNodeWithText("Schody").assertExists()
        compose.onNodeWithText("Budynek A-1, 1 piętro").assertExists()
    }

    /** Winda ma osobny arkusz, a pomieszczenie spoza planu nie ma arkusza. Wymagania: US16.1 */
    @Test
    fun skipsLiftAndUnknownRoom() {
        assertNull("winda bez tego arkusza", SIGN_PLAN.signOf("winda"))
        assertNull("pomieszczenie spoza planu", SIGN_PLAN.signOf("brak"))
    }

    /** Wiersz: przy czcionce 200% arkusz mieści nagłówek i miejsce. Wymagania: NFR-20 */
    @Test
    @Config(fontScale = 2f)
    fun matchesLargeFontMockup() {
        show("wc")
        compose.onRoot().captureRoboImage()
    }

    /** Zrzut arkusza toalety dostosowanej, motyw jasny. */
    @Test
    fun matchesLightMockup() {
        show("wc")
        compose.onRoot().captureRoboImage()
    }

    /** Zrzut arkusza toalety dostosowanej, motyw ciemny. */
    @Test
    @Config(qualifiers = "+night")
    fun matchesDarkMockup() {
        show("wc")
        compose.onRoot().captureRoboImage()
    }

    private fun show(id: String) {
        compose.setContent {
            DostepnyKampusTheme {
                Box(Modifier.fillMaxSize().background(MapGround)) {
                    ChosenPlanSheet(
                        id = id,
                        plan = SIGN_PLAN,
                        onClose = { closed++ },
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }
    }

    private companion object {
        val MapGround = Color(0xFFBEE3F2)
    }
}
