/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.entrance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.res.stringResource
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.marking.SeverityBadge
import io.github.dostepnykampus.feature.map.R
import io.github.dostepnykampus.feature.map.barrier.BarrierFacts
import io.github.dostepnykampus.feature.map.barrier.ChosenBarrier

/**
 * Część arkusza wejścia ze zgłoszeniami przyjętymi przy wejściu [shown]: każde z uciążliwością, typem, dniem
 * sprawdzenia i zdjęciem, w tych samych słowach co arkusz bariery.
 *
 * Wymagania: US2.2, US3.3, NFR-21
 */
@Composable
internal fun EntranceBarriers(shown: ShownEntrance) {
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.m)) {
        SheetHeading(stringResource(R.string.mapa_wejscie_bariery))
        shown.barriers.forEach { barrier ->
            key(barrier.id) {
                Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
                    SeverityBadge(barrier.severity)
                    BarrierFacts(
                        chosen = ChosenBarrier(barrier, building = null, entrance = null),
                        hasPlace = false,
                        titleStyle = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}
