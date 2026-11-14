/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.dostepnykampus.core.designsystem.GroupContainer
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.feature.map.R
import io.github.dostepnykampus.feature.map.barrier.BarrierFacts
import io.github.dostepnykampus.feature.map.barrier.ChosenBarrier
import io.github.dostepnykampus.feature.map.barrier.SeverityBadge

/**
 * Grupa „Bariery w budynku” na karcie: każde zgłoszenie przyjęte budynku z uciążliwością, typem, miejscem i dniem
 * sprawdzenia, w tych samych słowach co arkusz bariery na mapie. Karta bez zgłoszeń przyjętych nie ma grupy, więc
 * nie sugeruje braku barier.
 *
 * Wymagania: US2.2, US2.3, US2.4, US2.5, BR-17, NFR-21
 *
 * @param card karta budynku; grupa pojawia się, gdy [BuildingCardUiState.Ready.barriers] nie jest pusta
 */
@Composable
internal fun BarriersGroup(card: BuildingCardUiState.Ready) {
    if (card.barriers.isEmpty()) return
    val entranceNames = card.entrances.associate { it.entrance.id to it.entrance.name }
    GroupContainer(title = stringResource(R.string.karta_bariery)) {
        Column(verticalArrangement = Arrangement.spacedBy(Odstepy.m)) {
            card.barriers.forEach { barrier ->
                Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
                    SeverityBadge(barrier.severity)
                    BarrierFacts(ChosenBarrier(barrier, building = null, entrance = entranceNames[barrier.entranceId]))
                }
            }
        }
    }
}
