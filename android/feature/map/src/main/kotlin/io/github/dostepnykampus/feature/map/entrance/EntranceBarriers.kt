/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.entrance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.feature.map.R
import io.github.dostepnykampus.feature.map.barrier.BarrierFacts
import io.github.dostepnykampus.feature.map.barrier.ChosenBarrier
import io.github.dostepnykampus.feature.map.barrier.SeverityBadge

/**
 * Część arkusza wejścia ze zgłoszeniami przyjętymi przy wejściu [shown]: każde z uciążliwością, typem, dniem
 * sprawdzenia i zdjęciem, w tych samych słowach co arkusz bariery.
 *
 * Wymagania: US2.2, US3.3, PW-3, PW-13, NFR-21
 */
@Composable
internal fun EntranceBarriers(shown: ShownEntrance) {
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.m)) {
        Text(
            text = stringResource(R.string.mapa_wejscie_bariery),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = Typografia.wyroznienieWaga,
        )
        shown.barriers.forEach { barrier ->
            key(barrier.id) {
                Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
                    SeverityBadge(barrier.severity)
                    BarrierFacts(ChosenBarrier(barrier, building = null, entrance = null), hasPlace = false)
                }
            }
        }
    }
}
