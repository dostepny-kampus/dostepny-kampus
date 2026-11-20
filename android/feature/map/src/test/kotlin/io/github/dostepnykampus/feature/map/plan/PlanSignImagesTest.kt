/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.plan

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.designsystem.MapColors
import io.github.dostepnykampus.core.model.room.RoomCategory
import io.github.dostepnykampus.feature.map.style.PLAN_SIGN_DP
import io.github.dostepnykampus.feature.map.style.planSignImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Obrazy znaków windy, toalety dostosowanej i schodów na planie V1. Wymagania: US16.1, NFR-16 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PlanSignImagesTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun drawsDifferentSignPerCategory() {
        val images = planSignImages(colors = colors(), density = DENSITY)

        val names = listOf(RoomCategory.ELEVATOR, RoomCategory.ACCESSIBLE_TOILET, RoomCategory.STAIRCASE)
        assertEquals("Znak ma grupa bez znaku albo go brakuje", names.map(::planSignImage).toSet(), images.keys)
        assertEquals("Znaki mają różne boki", listOf(SIGN_PX), images.values.map { it.width }.distinct())
        val signs = images.values.toList()
        val pairs = signs.indices.flatMap { first -> (first + 1 until signs.size).map { first to it } }
        assertFalse("Dwa znaki planu wyglądają tak samo", pairs.any { (a, b) -> signs[a].sameAs(signs[b]) })
    }

    @Test
    fun paintsLightSignsWithTokens() {
        assertBackgrounds(colors())
    }

    @Test
    @Config(qualifiers = "+night")
    fun paintsDarkSignsWithTokens() {
        assertBackgrounds(colors())
    }

    private fun assertBackgrounds(colors: MapColors) {
        val images = planSignImages(colors = colors, density = DENSITY)
        val inside = (BACKGROUND_UNIT * SIGN_PX / SIGN_UNITS).toInt()
        val lift = images.getValue(planSignImage(RoomCategory.ELEVATOR)).getPixel(inside, SIGN_PX / 2)
        val stairs = images.getValue(planSignImage(RoomCategory.STAIRCASE)).getPixel(inside, SIGN_PX / 2)

        assertEquals("Tło znaku windy nie jest w kolorze tusz", colors.tusz.toArgb(), lift)
        assertEquals("Tło znaku schodów nie jest w kolorze papier", colors.papier.toArgb(), stairs)
    }

    private fun colors(): MapColors {
        var colors: MapColors? = null
        compose.setContent { DostepnyKampusTheme { colors = DostepnyKampusTheme.mapColors } }
        compose.waitForIdle()
        return checkNotNull(colors)
    }

    private companion object {
        const val DENSITY = 2f
        val SIGN_PX = (PLAN_SIGN_DP * 2).toInt()
        const val SIGN_UNITS = 34f
        const val BACKGROUND_UNIT = 6f
    }
}
