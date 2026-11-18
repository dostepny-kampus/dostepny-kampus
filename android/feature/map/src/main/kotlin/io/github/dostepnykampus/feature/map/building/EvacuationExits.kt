/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.core.model.building.EvacuationExit
import io.github.dostepnykampus.feature.map.R
import io.github.dostepnykampus.feature.map.overlay.MapControlIcons

/**
 * Wyjścia ewakuacyjne budynku z karty [card]: przycisk z liczbą wyjść, który rozwija i zwija ich listę, na start
 * zwiniętą. Wiersz wyjścia ma opis i szerokość drzwi, gdy są w danych, i nie jest dotykalny. Bez wyjść nic się nie
 * rysuje.
 *
 * Wymagania: US2.6, BR-27, NFR-17, NFR-18
 */
@Composable
internal fun EvacuationExits(card: BuildingCardUiState.Ready) {
    val exits = card.evacuationExits
    if (exits.isEmpty()) return
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val state = stringResource(if (isExpanded) R.string.karta_rozwiniete else R.string.karta_zwiniete)
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        OutlinedButton(
            onClick = { isExpanded = !isExpanded },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = CeleDotyku.minimum)
                    .semantics { stateDescription = state },
        ) {
            Text(text = stringResource(R.string.karta_wyjscia_ewakuacyjne, exits.size), modifier = Modifier.weight(1f))
            Icon(if (isExpanded) MapControlIcons.Up else MapControlIcons.Down, contentDescription = null)
        }
        if (isExpanded) {
            val line = MaterialTheme.colorScheme.outlineVariant
            exits.forEachIndexed { index, exit ->
                if (index > 0) HorizontalDivider(Modifier.padding(vertical = Odstepy.xs), color = line)
                ExitRow(exit)
            }
        }
    }
}

@Composable
private fun ExitRow(exit: EvacuationExit) {
    Column(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(Odstepy.xs),
    ) {
        if (exit.description.isNotEmpty()) {
            Text(
                text = exit.description,
                fontWeight = Typografia.wyroznienieWaga,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        if (exit.doorWidthCm > 0) {
            val door = stringResource(R.string.karta_szerokosc_drzwi, exit.doorWidthCm)
            Text(text = door, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
