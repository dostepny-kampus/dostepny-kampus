/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.room

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.feature.map.R

/** Nagłówek drogi z wyborem kroku karty [card]. Wymagania: US18.2, NFR-17, NFR-18 */
@Composable
internal fun RoomWayHeader(card: RoomCard, onStep: ((Int) -> Unit)?) {
    val step = card.step
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.mapa_sala_droga),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        if (onStep != null && card.stepCount > 0) {
            if (step == null) {
                TextButton(onClick = { onStep(0) }) { Text(stringResource(R.string.mapa_sala_pokaz_kroki)) }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = stringResource(R.string.mapa_sala_krok_z, step + 1, card.stepCount))
                    StepArrow(glyph = "‹", label = R.string.mapa_sala_krok_wstecz, enabled = card.hasPrevious) {
                        onStep(step - 1)
                    }
                    StepArrow(glyph = "›", label = R.string.mapa_sala_krok_dalej, enabled = card.hasNext) {
                        onStep(step + 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepArrow(glyph: String, label: Int, enabled: Boolean, onClick: () -> Unit) {
    val name = stringResource(label)
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.semantics { contentDescription = name },
    ) {
        Text(text = glyph, style = MaterialTheme.typography.headlineSmall)
    }
}
