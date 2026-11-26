/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.room

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.feature.map.R

/**
 * Nagłówek listy kroków karty [card] z „Pokaż po kolei”, które wywołuje [onStart]; bez [onStart] albo bez kroków
 * drogi sam nagłówek.
 *
 * Wymagania: US18.2, NFR-17, NFR-18
 */
@Composable
internal fun RoomWayHeader(card: RoomCard, onStart: (() -> Unit)?) {
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
        if (onStart != null && card.stepCount > 0) {
            TextButton(onClick = onStart, modifier = Modifier.heightIn(min = CeleDotyku.minimum)) {
                Text(stringResource(R.string.mapa_sala_pokaz_kroki))
            }
        }
    }
}
