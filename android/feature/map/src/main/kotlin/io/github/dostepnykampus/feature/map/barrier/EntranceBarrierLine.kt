/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.barrier

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.core.designsystem.marking.SeverityIcon
import io.github.dostepnykampus.core.designsystem.marking.nameRes
import io.github.dostepnykampus.feature.map.R

/**
 * Wiersz bariery w wierszu wejścia na liście „Wejścia”: znak i słowo uciążliwości, a po dwukropku typ bariery,
 * np. „Tylko z pomocą: ciężkie drzwi”.
 *
 * Wymagania: US2.2, BR-25
 */
@Composable
internal fun EntranceBarrierLine(barrier: AcceptedBarrier) {
    val severity = stringResource(barrier.severity.nameRes())
    val type = barrier.type?.run { stringResource(nameRes()).replaceFirstChar(Char::lowercaseChar) }
    Row(
        horizontalArrangement = Arrangement.spacedBy(Odstepy.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SeverityIcon(barrier.severity)
        Text(
            text = type?.let { stringResource(R.string.karta_wejscie_bariera, severity, it) } ?: severity,
            fontWeight = Typografia.wyroznienieWaga,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
