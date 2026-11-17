/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.feature.map.R
import io.github.dostepnykampus.feature.map.overlay.SheetCloseButton

/**
 * Nagłówek karty V2 w trybie [mode]: symbol i nazwa z [card] albo miejsce na nie w trakcie wczytywania, pod nimi
 * wiersz o wejściach; w [CardMode.Compact] obok „Zamknij”, które wywołuje [onClose].
 *
 * Wymagania: US2, US2.8, NFR-17, NFR-18, NFR-53
 */
@Composable
internal fun CardHeader(symbol: String, card: BuildingCardUiState, mode: CardMode, onClose: () -> Unit) {
    val isCompact = mode == CardMode.Compact
    if (!isCompact && card !is BuildingCardUiState.Ready) return
    Row(horizontalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        Box(Modifier.weight(1f)) { HeaderOrSpace(symbol = symbol, card = card, isFullScreen = !isCompact) }
        if (isCompact) SheetCloseButton(onClose = onClose)
    }
}

@Composable
private fun HeaderOrSpace(symbol: String, card: BuildingCardUiState, isFullScreen: Boolean) {
    val ready = card as? BuildingCardUiState.Ready
    if (ready == null && !card.isWaiting(isFullScreen)) return
    Column(
        modifier = if (ready == null) Modifier.alpha(0f).clearAndSetSemantics {} else Modifier,
        verticalArrangement = Arrangement.spacedBy(Odstepy.s),
    ) {
        if (ready != null) {
            SymbolOverName(symbol = symbol, name = ready.name)
            EntranceSummaryLine(entranceSummary(ready.entrances))
        } else {
            Column {
                Text(text = symbol, style = MaterialTheme.typography.displayLarge)
                Text(text = " ", style = MaterialTheme.typography.headlineMedium)
            }
            EntranceSummaryLine(EntranceSummary.NoData)
        }
    }
}

@Composable
private fun EntranceSummaryLine(summary: EntranceSummary) {
    if (summary == EntranceSummary.NoData) {
        Text(
            text = stringResource(R.string.karta_wiersz_brak_danych),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        EntranceSummaryRow(summary)
    }
}

internal fun BuildingCardUiState.isWaiting(isFullScreen: Boolean): Boolean =
    this == BuildingCardUiState.Loading && !isFullScreen

@Composable
private fun SymbolOverName(symbol: String, name: String?) {
    val spoken =
        name?.let { stringResource(R.string.karta_czytaj, symbol, it) } ?: stringResource(R.string.mapa_budynek, symbol)
    val focus = remember { FocusRequester() }
    Column(
        modifier =
            Modifier
                .semantics(mergeDescendants = true) { contentDescription = spoken }
                .focusRequester(focus)
                .focusable(),
    ) {
        Text(text = symbol, style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
        name?.let { Text(text = it, style = MaterialTheme.typography.headlineMedium) }
    }
    LaunchedEffect(focus) { withFrameMillis { focus.requestFocus() } }
}
