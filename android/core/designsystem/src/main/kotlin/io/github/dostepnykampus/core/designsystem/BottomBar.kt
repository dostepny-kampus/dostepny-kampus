/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
private val ShortWordBeforeSpace = Regex("""(?<=(^|\s)[aiouwzAIOUWZ]) """)

/** Pozycja paska dolnego: napis [label] pod ikoną [icon]. */
data class BottomBarItem(val label: String, val icon: ImageVector)

/** Pozycje paska dolnego od lewej; w napisach po jednoliterowych słowach stoi twarda spacja. */
@Immutable
class BottomBarItems(items: List<BottomBarItem>) {
    internal val entries = items.map { it.copy(label = keepShortWordsWithNext(it.label)) }
}

/**
 * Pasek dolny z pozycjami [items] w jednym rzędzie, wybrana [selectedIndex]; dotknięcie pozycji wywołuje [onSelect]
 * z jej numerem. Stoi poza przewijaną treścią (`bottomBar` w `Scaffold`), więc nigdy się nie chowa (NFR-52).
 * TalkBack czyta napis, rolę karty i stan wybrany (NFR-18).
 *
 * Napisy (reguła 8, NFR-20) mają jeden rozmiar i najwyżej dwa wiersze: rosną z czcionką, dopóki najdłuższe słowo
 * wszystkich napisów mieści się w pozycji, ale co najmniej 1,3 raza. Pozycje dzieli [Odstepy.s] od siebie i od
 * krawędzi. Po jednoliterowym słowie stoi twarda spacja, więc „Konto i praca” łamie się jako „Konto / i praca”.
 *
 * Tokeny: tło `surfaceContainerLow`, górna krawędź [Obramowania.ramka] w kolorze `outlineVariant`; napis
 * `labelMedium` w kolorze `onSurfaceVariant`, wybrany `onSurface` z [Typografia.wyroznienieWaga]; ikona
 * [Ikony.rozmiar] w kolorze `onSurfaceVariant`, wybrana `onPrimaryContainer` na wskaźniku `primaryContainer`.
 */
@Composable
fun BottomBar(items: BottomBarItems, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val iconSize = Modifier.size(Ikony.rozmiar)
    Column(modifier = modifier) {
        HorizontalDivider(thickness = Obramowania.ramka, color = colors.outlineVariant)
        NavigationBar(
            containerColor = colors.surfaceContainerLow,
            windowInsets = NavigationBarDefaults.windowInsets.add(WindowInsets(left = Odstepy.s, right = Odstepy.s)),
        ) {
            items.entries.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onSelect(index) },
                    icon = { Icon(imageVector = item.icon, contentDescription = null, modifier = iconSize) },
                    label = { BottomBarLabel(text = item.label, items = items, selected = isSelected) },
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
        }
    }
}

internal fun keepShortWordsWithNext(text: String): String = text.replace(ShortWordBeforeSpace, "\u00A0")

@Composable
private fun BottomBarLabel(text: String, items: BottomBarItems, selected: Boolean) {
    val style = MaterialTheme.typography.labelMedium
    BoxWithConstraints {
        val fontSize = sharedLabelSize(items, style.copy(fontWeight = Typografia.wyroznienieWaga), constraints.maxWidth)
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
private fun sharedLabelSize(items: BottomBarItems, style: TextStyle, maxWidth: Int): TextUnit {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    return remember(items, style, maxWidth, density) {
        with(density) {
            val fullPx = style.fontSize.toPx()
            val minPx = minOf(a = fullPx, b = style.fontSize.value * density.density * LABEL_MIN_GROWTH_AT_LARGE_FONT)
            val widest = items.entries.flatMap { it.label.split(' ') }.maxOf { measurer.measure(it, style).size.width }
            val fitPx = if (widest > maxWidth) fullPx * maxWidth / widest else fullPx
            fitPx.coerceIn(minimumValue = minPx, maximumValue = fullPx).toSp()
        }
    }
}
