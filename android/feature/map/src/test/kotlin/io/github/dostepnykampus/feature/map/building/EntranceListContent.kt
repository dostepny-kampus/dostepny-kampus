/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.building.Entrance

/**
 * Karta [symbol] gotowa do pokazania z wejściami [entrances] w kolejności listy i barierami [barriers] przy nich.
 *
 * Wymagania: US2.1, US2.2
 */
internal fun readyCard(
    symbol: String,
    entrances: List<Entrance>,
    barriers: List<AcceptedBarrier> = emptyList(),
): BuildingCardUiState.Ready =
    BuildingCardUiState.Ready(symbol = symbol, name = null, entrances = orderedEntrances(entrances, barriers))

/** Grupa „Wejścia” karty [card] na tle karty, w motywie aplikacji. Wymagania: US2.1 */
@Composable
internal fun EntranceListContent(card: BuildingCardUiState.Ready) {
    OnCardSurface { EntrancesGroup(card = card, withAssessment = true, onShowEntrance = {}, onReport = {}) }
}

/** Nagłówek rozwiniętej karty [card] na tle karty, w motywie aplikacji. Wymagania: US2.8, US3.1 */
@Composable
internal fun CardHeaderContent(card: BuildingCardUiState.Ready) {
    OnCardSurface { CardHeader(symbol = card.symbol, card = card, mode = CardMode.Expanded, onClose = {}) }
}

@Composable
private fun OnCardSurface(content: @Composable () -> Unit) {
    DostepnyKampusTheme {
        Column(Modifier.background(MaterialTheme.colorScheme.surface).padding(16.dp)) { content() }
    }
}
