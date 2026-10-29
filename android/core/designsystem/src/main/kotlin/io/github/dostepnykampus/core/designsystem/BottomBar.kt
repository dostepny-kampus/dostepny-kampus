/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em

private const val LABEL_MIN_GROWTH_AT_LARGE_FONT = 1.3f

/**
 * Pasek dolny nawigacji z pozycjami [content] ([BottomBarItem]) w jednym rzędzie (wymagania wspólne, reguła 8).
 * Wywołujący stawia go poza przewijaną treścią, np. w `bottomBar` `Scaffold`, więc pasek nigdy się nie chowa (NFR-52).
 *
 * Tokeny: tło `surfaceContainerLow`, górna krawędź [Obramowania.ramka] w kolorze `outlineVariant`.
 */
@Composable
fun BottomBar(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Column(modifier = modifier) {
        HorizontalDivider(thickness = Obramowania.ramka, color = MaterialTheme.colorScheme.outlineVariant)
        NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow, content = content)
    }
}

/**
 * Pozycja paska dolnego: ikona [icon] nad napisem [label]. TalkBack czyta napis, rolę karty i stan [selected]
 * (NFR-18); ikona jest ozdobna. Dotknięcie wywołuje [onClick].
 *
 * Napis ma najwyżej dwa wiersze i rośnie z czcionką telefonu, dopóki jego najdłuższe słowo mieści się w pozycji,
 * ale co najmniej 1,3 raza: odstępstwo od pełnych 200 % z `dostepnosc.md` (reguła 8, NFR-20).
 *
 * Tokeny: napis `labelMedium` w kolorze `onSurfaceVariant`, wybrany `onSurface` z [Typografia.wyroznienieWaga];
 * ikona [Ikony.rozmiar] w kolorze `onSurfaceVariant`, wybrana `onPrimaryContainer` na wskaźniku `primaryContainer`.
 */
@Composable
fun RowScope.BottomBarItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(imageVector = icon, contentDescription = null) },
        modifier = modifier,
        label = { BottomBarLabel(text = label, selected = selected) },
        colors =
            NavigationBarItemDefaults.colors(
                selectedIconColor = colors.onPrimaryContainer,
                selectedTextColor = colors.onSurface,
                indicatorColor = colors.primaryContainer,
                unselectedIconColor = colors.onSurfaceVariant,
                unselectedTextColor = colors.onSurfaceVariant,
            ),
    )
}

@Composable
private fun BottomBarLabel(text: String, selected: Boolean) {
    val style = MaterialTheme.typography.labelMedium
    val widestStyle = style.copy(fontWeight = Typografia.wyroznienieWaga)
    BoxWithConstraints {
        val fontSize = labelSizeWithWholeWords(text, widestStyle, constraints.maxWidth)
        Text(
            text = text,
            style = style.copy(fontSize = fontSize, lineHeight = (style.lineHeight.value / style.fontSize.value).em),
            fontWeight = if (selected) Typografia.wyroznienieWaga else null,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Composable
private fun labelSizeWithWholeWords(text: String, style: TextStyle, maxWidth: Int): TextUnit {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    return remember(text, style, maxWidth, density) {
        with(density) {
            val fullPx = style.fontSize.toPx()
            val minPx = minOf(a = fullPx, b = style.fontSize.value * density.density * LABEL_MIN_GROWTH_AT_LARGE_FONT)
            val widest = text.split(' ').maxOf { measurer.measure(it, style).size.width }
            val fitPx = if (widest > maxWidth) fullPx * maxWidth / widest else fullPx
            fitPx.coerceIn(minimumValue = minPx, maximumValue = fullPx).toSp()
        }
    }
}
