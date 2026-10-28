/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

private val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }

private val LocalMapColors = staticCompositionLocalOf { LightMapColors }

private val AtkinsonHyperlegibleNext =
    FontFamily(
        Font(R.font.atkinson_hyperlegible_next_regular, FontWeight.Normal),
        Font(R.font.atkinson_hyperlegible_next_medium, FontWeight.Medium),
        Font(R.font.atkinson_hyperlegible_next_semibold, FontWeight.SemiBold),
        Font(R.font.atkinson_hyperlegible_next_bold, FontWeight.Bold),
        Font(R.font.atkinson_hyperlegible_next_extrabold, FontWeight.ExtraBold),
    )

private val AppTypography = typography(AtkinsonHyperlegibleNext)

/**
 * Motyw aplikacji z tokenów wizualnych (`docs/docs/ux/tokeny.yml`, decyzje projektu nr 48 i 50).
 *
 * Schemat kolorów jasny albo ciemny według [darkTheme], domyślnie według ustawienia systemu. Paleta jest
 * stała: bez kolorów dynamicznych z tapety, bo kontrast par z tokenów (NFR-16) jest sprawdzony tylko dla
 * tej palety. Typografia w krój Atkinson Hyperlegible Next wbudowany w aplikację, rozmiary w sp: tekst
 * rośnie z ustawieniem czcionki telefonu (NFR-20). Kolory oznaczeń wejścia i mapy daje
 * [DostepnyKampusTheme.statusColors] i [DostepnyKampusTheme.mapColors].
 */
@Composable
fun DostepnyKampusTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalStatusColors provides if (darkTheme) DarkStatusColors else LightStatusColors,
        LocalMapColors provides if (darkTheme) DarkMapColors else LightMapColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

/** Role tokenów spoza Material 3 dla bieżącego motywu; poza [DostepnyKampusTheme] wartości motywu jasnego. */
object DostepnyKampusTheme {
    /** Kontenery oznaczeń wejścia i kolor ich kształtu i słowa (tokeny `status…Container`, NFR-21). */
    val statusColors: StatusColors
        @Composable @ReadOnlyComposable
        get() = LocalStatusColors.current

    /** Paleta mapy i planu piętra (sekcja `mapa.kolory`) do stylu mapy. */
    val mapColors: MapColors
        @Composable @ReadOnlyComposable
        get() = LocalMapColors.current
}
