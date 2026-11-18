/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem.bar

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextAlign
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.designsystem.Obramowania
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.core.designsystem.Wysokosci

/**
 * Przypięty pasek czynności nad paskiem dolnym, np. „Zgłoś barierę” i „Anuluj”.
 *
 * Tokeny: tło `surface`, cień [Wysokosci.pasekCzynnosci], margines [Odstepy.m], odstęp między przyciskami
 * [CeleDotyku.odstep]. Szerokość przycisków [content] ustala wywołujący: [PinnedActionScope.weight] w wierszu,
 * `fillMaxWidth()` albo szerokość napisu. Przycisk, który się nie mieści, np. przy dużej czcionce, przechodzi do
 * następnego wiersza; słowo napisu przycisku z wagą nie łamie się w środku.
 *
 * Wymagania: NFR-20
 */
@Composable
public fun PinnedActionBar(modifier: Modifier = Modifier, content: @Composable PinnedActionScope.() -> Unit) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = Wysokosci.pasekCzynnosci,
    ) {
        Layout(
            content = { PinnedActionScopeInstance.content() },
            modifier = Modifier.fillMaxWidth().padding(Odstepy.m),
            measurePolicy = pinnedActionsPolicy,
        )
    }
}

/**
 * Przycisk przypiętego paska czynności ([PinnedActionBar]) rodzaju [kind] z napisem [text]; szerokość nadaje
 * [modifier]. Wysokość co najmniej [CeleDotyku.przypiety], rośnie z tekstem. Kolory rodzajów opisuje
 * [PinnedButtonKind]; w ciemnym motywie czynność główną od tonalnej odróżnia gruba ramka, bo jasne tło `primary`
 * świeciłoby.
 *
 * Wymagania: NFR-17, NFR-20, NFR-16, NFR-21
 */
@Composable
public fun PinnedButton(
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
            val colorScheme = MaterialTheme.colorScheme
            val isDarkTheme = DostepnyKampusTheme.isDark
            Button(
                onClick = onClick,
                modifier = buttonModifier,
                shape = shape,
                colors =
                    if (isDarkTheme) {
                        ButtonDefaults.buttonColors(
                            containerColor = colorScheme.primaryContainer,
                            contentColor = colorScheme.onPrimaryContainer,
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    },
                border = if (isDarkTheme) BorderStroke(Obramowania.wypelniony, colorScheme.primary) else null,
            ) { label() }
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
