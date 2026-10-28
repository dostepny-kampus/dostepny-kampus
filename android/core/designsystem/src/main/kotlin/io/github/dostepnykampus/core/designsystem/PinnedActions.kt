/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign

/**
 * Przypięty pasek czynności nad paskiem dolnym, np. „Sprawdź, którędy wejść” i „Pokaż na mapie” (V2).
 *
 * Tokeny: tło `surface`, cień [Wysokosci.pasekCzynnosci], margines [Odstepy.m], odstęp między przyciskami
 * [CeleDotyku.odstep]. Szerokość przycisków [content] ustala wywołujący, np. `Modifier.weight` w wierszu albo
 * `fillMaxWidth()`; przyciski, które się nie mieszczą, np. przy dużej czcionce (NFR-20), przechodzą do następnego
 * wiersza.
 */
@Composable
fun PinnedActionBar(modifier: Modifier = Modifier, content: @Composable FlowRowScope.() -> Unit) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = Wysokosci.pasekCzynnosci,
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(Odstepy.m),
            horizontalArrangement = Arrangement.spacedBy(CeleDotyku.odstep),
            verticalArrangement = Arrangement.spacedBy(CeleDotyku.odstep),
            content = content,
        )
    }
}

/**
 * Przycisk przypiętego paska czynności ([PinnedActionBar]) rodzaju [kind], z napisem [text]. Szerokość
 * nadaje [modifier].
 *
 * Tokeny: wysokość co najmniej [CeleDotyku.przypiety] (rośnie z tekstem, NFR-17, NFR-20), zaokrąglenie
 * [Zaokraglenia.bardzoDuze], napis `labelLarge` z [Typografia.wyroznienieWaga].
 */
@Composable
fun PinnedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: PinnedButtonKind = PinnedButtonKind.Filled,
) {
    val buttonModifier = modifier.heightIn(min = CeleDotyku.przypiety)
    val shape = MaterialTheme.shapes.extraLarge
    val label: @Composable () -> Unit = {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = Typografia.wyroznienieWaga,
            textAlign = TextAlign.Center,
        )
    }
    when (kind) {
        PinnedButtonKind.Filled -> {
            Button(onClick = onClick, modifier = buttonModifier, shape = shape) { label() }
        }

        PinnedButtonKind.Tonal -> {
            FilledTonalButton(onClick = onClick, modifier = buttonModifier, shape = shape) { label() }
        }

        PinnedButtonKind.Outlined -> {
            OutlinedButton(
                onClick = onClick,
                modifier = buttonModifier,
                shape = shape,
                border = BorderStroke(Obramowania.ramka, MaterialTheme.colorScheme.outline),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            ) { label() }
        }
    }
}
