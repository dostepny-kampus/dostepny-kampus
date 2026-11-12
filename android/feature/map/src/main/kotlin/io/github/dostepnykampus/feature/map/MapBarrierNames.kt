/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.data.barriers.AcceptedBarrier
import io.github.dostepnykampus.core.designsystem.nameRes

private val NodeSize = 1.dp

/** Bariery [barriers], których znaki mapa pokazuje; lista w klasie stabilnej dla Compose. */
@Immutable
internal data class ShownBarriers(val barriers: List<AcceptedBarrier>)

/**
 * Węzły dla TalkBack: natywna mapa nie opisuje znaków barier, więc każda bariera z [barriers] ma tu nazwę, np.
 * „Blokada: schody przy wejściu, budynek A-1” (NFR-18). Węzły nie leżą w punktach znaków i nic nie rysują;
 * TalkBack dochodzi do nich przesuwaniem po kolei, tak jak do zaznaczonego wejścia (decyzja 135), a pełną treść
 * bariery ma lista budynków (NFR-19).
 */
@Composable
internal fun BarrierDescriptions(barriers: ShownBarriers, modifier: Modifier = Modifier) {
    Column(modifier) {
        barriers.barriers.forEach { barrier ->
            key(barrier.id) {
                val text = barrierName(barrier)
                Spacer(Modifier.size(NodeSize).semantics { contentDescription = text })
            }
        }
    }
}

@Composable
private fun barrierName(barrier: AcceptedBarrier): String {
    val severity = stringResource(barrier.severity.nameRes())
    val type = barrier.type?.run { stringResource(nameRes()).replaceFirstChar(Char::lowercaseChar) }
    val what = if (type == null) severity else stringResource(R.string.mapa_bariera_typ, severity, type)
    val symbol = barrier.buildingSymbol
    return if (symbol == null) {
        stringResource(R.string.mapa_bariera_poza_budynkiem, what)
    } else {
        stringResource(R.string.mapa_bariera_w_budynku, what, symbol)
    }
}
