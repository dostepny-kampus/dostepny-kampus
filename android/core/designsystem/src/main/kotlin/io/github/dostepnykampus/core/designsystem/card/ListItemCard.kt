/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem.card

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.dostepnykampus.core.designsystem.Obramowania
import io.github.dostepnykampus.core.designsystem.Odstepy

/**
 * Karta pozycji listy na całą szerokość: tło `surfaceContainerLowest`, ramka [Obramowania.ramka] w kolorze
 * `outline`, kształt `shapes.large`, wcięcie [Odstepy.m]. Elementy [content] układają się w kolumnie co
 * [Odstepy.s]; czynności karty ([CardAction]) stoją na jej końcu.
 *
 * Wymagania: NFR-16, NFR-21, US8.1, US10.4, US11.1, US15.1
 */
@Composable
public fun ListItemCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(Obramowania.ramka, MaterialTheme.colorScheme.outline),
    ) {
        CardColumn(content)
    }
}

/** Kolumna treści karty i panelu: wcięcie [Odstepy.m], elementy [content] co [Odstepy.s]. */
@Composable
internal fun CardColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.padding(Odstepy.m),
        verticalArrangement = Arrangement.spacedBy(Odstepy.s),
        content = content,
    )
}
