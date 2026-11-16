/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.style

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.map.camera.styleColors
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Barwy granicy terenu z tokenów motywu: linia `granica_terenu` z obwódką `granica_terenu_obwodka`, a poza granicą
 * kolor `background` z kryciem `mapa.krycie.teren_poza`, w motywie jasnym i ciemnym (tokeny.yml).
 *
 * Wymagania: BR-G5, NFR-16
 */
@RunWith(RobolectricTestRunner::class)
class MapStyleThemeColorsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun takesLightEdgeFromTokens() {
        val colors = themed(isDark = false)
        assertEquals("Linia granicy w motywie jasnym", "#181C1D", colors.edge)
        assertEquals("Obwódka linii w motywie jasnym", "#FFFFFF", colors.edgeHalo)
        assertEquals("Tło motywu jasnego poza terenem", "#E8EEF0", colors.outside)
        assertEquals("Krycie poza terenem w motywie jasnym", 0.75, colors.outsideOpacity, 0.0)
    }

    @Test
    fun takesDarkEdgeFromTokens() {
        val colors = themed(isDark = true)
        assertEquals("Linia granicy w motywie ciemnym", "#D5DADC", colors.edge)
        assertEquals("Obwódka linii w motywie ciemnym", "#20272B", colors.edgeHalo)
        assertEquals("Tło motywu ciemnego poza terenem", "#252B2F", colors.outside)
        assertEquals("Krycie poza terenem w motywie ciemnym", 0.65, colors.outsideOpacity, 0.0)
    }

    private fun themed(isDark: Boolean): MapStyleColors {
        var colors: MapStyleColors? = null
        compose.setContent {
            DostepnyKampusTheme(darkTheme = isDark) { colors = DostepnyKampusTheme.mapColors.styleColors() }
        }
        compose.waitForIdle()
        return requireNotNull(colors)
    }
}
