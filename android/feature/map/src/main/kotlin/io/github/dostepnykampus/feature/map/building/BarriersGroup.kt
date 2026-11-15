/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
 * sprawdzenia, w tych samych słowach co arkusz bariery na mapie. Gdy dane są, a budynek nie ma zgłoszeń przyjętych,
 * grupa jest w stanie pustym; bez danych o barierach grupy nie ma.
 * TODO(#2042): przycisk zgłoszenia bariery w stanie pustym.
 *
 * Wymagania: US2.2, US2.3, US2.4, US2.5, US2.7, BR-17, NFR-21
 *
 * @param card karta budynku; `null` w [BuildingCardUiState.Ready.barriers] oznacza brak grupy
 */
@Composable
internal fun BarriersGroup(card: BuildingCardUiState.Ready) {
    val barriers = card.barriers ?: return
    if (barriers.isEmpty()) {
        EmptyBarriers()
        return
    }
    val entranceNames = card.entrances.associate { it.entrance.id to it.entrance.name }
    GroupContainer(title = stringResource(R.string.karta_bariery)) {
        Column(verticalArrangement = Arrangement.spacedBy(Odstepy.m)) {
            barriers.forEach { barrier ->
                Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
                    SeverityBadge(barrier.severity)
                    BarrierFacts(ChosenBarrier(barrier, building = null, entrance = entranceNames[barrier.entranceId]))
                }
            }
        }
    }
}

@Composable
private fun EmptyBarriers() {
    GroupContainer(title = stringResource(R.string.karta_bariery)) {
        Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
            Text(
                text = stringResource(R.string.karta_bariery_brak),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.karta_bariery_brak_opis),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
