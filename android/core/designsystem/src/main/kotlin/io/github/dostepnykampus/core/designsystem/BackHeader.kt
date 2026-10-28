/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics

/**
 * Nagłówek ekranu: przycisk „Wstecz” (słowo bez ikony, zawsze w tym samym miejscu, NFR-15) i tytuł ekranu
 * [title], oznaczony dla TalkBack jako nagłówek. Dotknięcie „Wstecz” wywołuje [onBack].
 *
 * Tokeny: tło `surface`, margines [Odstepy.m], przycisk obrysowany z napisem `primary` i ramką
 * [Obramowania.ramka] w kolorze `outline`, zaokrąglenie [Zaokraglenia.kapsula], wysokość co najmniej
 * [CeleDotyku.minimum] (NFR-17), tytuł `titleLarge`. Tytuł stoi zawsze obok przycisku i zawija się w swojej
 * kolumnie, np. przy dużej czcionce (NFR-20).
 */
@Composable
fun BackHeader(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Odstepy.m, vertical = Odstepy.s),
            horizontalArrangement = Arrangement.spacedBy(Odstepy.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.heightIn(min = CeleDotyku.minimum),
                shape = RoundedCornerShape(Zaokraglenia.kapsula),
                border = BorderStroke(Obramowania.ramka, MaterialTheme.colorScheme.outline),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            ) {
                Text(
                    text = stringResource(R.string.wstecz),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = Typografia.wyroznienieWaga,
                )
            }
            Text(
                text = title,
                modifier = Modifier.weight(1f).semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}
