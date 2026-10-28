/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics

/**
 * Grupa na arkuszu, np. „Wejścia” albo „Bariery w budynku”: nagłówek grupy i kontener z jej treścią.
 *
 * Tokeny: odstęp [Odstepy.l] nad nagłówkiem, nagłówek `titleLarge` w kolorze `onSurface`, oznaczony dla
 * TalkBack jako nagłówek, kontener `surfaceContainerLowest` z ramką [Obramowania.ramka] w kolorze `outline`,
 * zaokrąglenie [Zaokraglenia.duze], wcięcie treści [Odstepy.m]. Treść [content] układa się w kolumnie.
 */
@Composable
fun GroupContainer(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = modifier.padding(top = Odstepy.l)) {
        Text(
            text = title,
            modifier = Modifier.padding(bottom = Odstepy.s).semantics { heading() },
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            border = BorderStroke(Obramowania.ramka, MaterialTheme.colorScheme.outline),
        ) {
            Column(modifier = Modifier.padding(horizontal = Odstepy.m, vertical = Odstepy.s), content = content)
        }
    }
}
