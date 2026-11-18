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
import io.github.dostepnykampus.core.designsystem.GroupFrame
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.core.model.building.EvacuationExit
import io.github.dostepnykampus.feature.map.R
import io.github.dostepnykampus.feature.map.overlay.MapControlIcons

/**
 * Wyjścia ewakuacyjne budynku z karty [card]: przycisk z liczbą wyjść, który rozwija i zwija ich listę, na start
 * zwiniętą; napis przycisku mówi, co zrobi następne dotknięcie. Lista stoi w kontenerze grupy. Wiersz wyjścia jak
 * wiersz wejścia bez nazwy: pogrubione „Wyjście ewakuacyjne”, pod nim [exitDetail] i szerokość drzwi, gdy są w danych;
 * wiersz nie jest dotykalny. Bez wyjść nic się nie rysuje.
 *
 * Wymagania: US2.6, BR-27, NFR-17, NFR-18
 */
@Composable
internal fun EvacuationExits(card: BuildingCardUiState.Ready) {
    val exits = card.evacuationExits
    if (exits.isEmpty()) return
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val state = stringResource(if (isExpanded) R.string.karta_rozwiniete else R.string.karta_zwiniete)
    val label = if (isExpanded) R.string.karta_wyjscia_ukryj else R.string.karta_wyjscia_ewakuacyjne
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        OutlinedButton(
            onClick = { isExpanded = !isExpanded },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = CeleDotyku.minimum)
                    .semantics { stateDescription = state },
        ) {
            Text(text = stringResource(label, exits.size), modifier = Modifier.weight(1f))
            Icon(if (isExpanded) MapControlIcons.Up else MapControlIcons.Down, contentDescription = null)
        }
        if (isExpanded) {
            GroupFrame {
                val line = MaterialTheme.colorScheme.outlineVariant
                exits.forEachIndexed { index, exit ->
                    if (index > 0) HorizontalDivider(Modifier.padding(vertical = Odstepy.s), color = line)
                    ExitRow(exit)
                }
            }
        }
    }
}

private const val SENTENCE_ENDS = ".!?"

/**
 * Reszta opisu wyjścia [description] z danych po początkowym [title] („Wyjście ewakuacyjne”), od wielkiej litery
 * i z kropką na końcu; opis bez tego początku w całości. Pusty, gdy po [title] nic nie ma.
 *
 * Wymagania: US2.6, BR-27
 */
internal fun exitDetail(description: String, title: String): String {
    val isTitled =
        description.startsWith(title, ignoreCase = true) && description.getOrNull(title.length)?.isLetter() != true
    val rest = if (isTitled) description.substring(title.length) else description
    val detail = rest.trim().trimStart(':', '-', '–', ',').trim()
    if (detail.isEmpty()) return ""
    val capital = detail.replaceFirstChar { it.uppercaseChar() }
    return if (capital.last() in SENTENCE_ENDS) capital else "$capital."
}

@Composable
private fun ExitRow(exit: EvacuationExit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = CeleDotyku.minimum)
                .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(Odstepy.xs),
    ) {
        val title = stringResource(R.string.karta_wyjscie_ewakuacyjne)
        Text(text = title, fontWeight = Typografia.wyroznienieWaga, style = MaterialTheme.typography.titleMedium)
        val detail = exitDetail(description = exit.description, title = title)
        if (detail.isNotEmpty()) Text(text = detail, style = MaterialTheme.typography.bodyLarge)
        if (exit.doorWidthCm > 0) {
            val door = stringResource(R.string.karta_szerokosc_drzwi, exit.doorWidthCm)
            Text(text = door, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
