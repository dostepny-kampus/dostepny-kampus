/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.barrier

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
import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
import io.github.dostepnykampus.core.designsystem.marking.nameRes
import io.github.dostepnykampus.feature.map.R

private val NodeSize = 1.dp

/** Bariery [barriers], których znaki mapa pokazuje; lista w klasie stabilnej dla Compose. */
@Immutable
internal data class ShownBarriers(val barriers: List<AcceptedBarrier>)

/**
 * Węzły dla TalkBack: natywna mapa nie opisuje znaków barier, więc każda bariera z [barriers] ma tu nazwę, np.
 * „Blokada: schody przy wejściu, budynek A-1”. Gdy pokazany jest plan budynku [planBuilding], bariera tego budynku
 * bez piętra dostaje dopisek „piętro nieznane”. Węzły nie leżą w punktach znaków i nic nie rysują.
 *
 * Wymagania: NFR-18, NFR-19, US16.1, BR-14
 */
@Composable
internal fun BarrierDescriptions(
    barriers: ShownBarriers,
    modifier: Modifier = Modifier,
    planBuilding: String? = null,
    selected: String? = null,
) {
    Column(modifier) {
        barriers.barriers.forEach { barrier ->
            key(barrier.id) {
                val text = barrierName(barrier, planBuilding, isSelected = barrier.id == selected)
                Spacer(Modifier.size(NodeSize).semantics { contentDescription = text })
            }
        }
    }
}

@Composable
private fun barrierName(barrier: AcceptedBarrier, planBuilding: String?, isSelected: Boolean): String {
    val severity = stringResource(barrier.severity.nameRes())
    val type = barrier.type?.run { stringResource(nameRes()).replaceFirstChar(Char::lowercaseChar) }
    val what =
        when {
            isSelected && type != null -> stringResource(R.string.mapa_zaznaczona_bariera_typ, severity, type)
            isSelected -> stringResource(R.string.mapa_zaznaczona_bariera, severity)
            type != null -> stringResource(R.string.mapa_bariera_typ, severity, type)
            else -> severity
        }
    val symbol = barrier.buildingSymbol
    val place =
        if (symbol == null) {
            stringResource(R.string.mapa_bariera_poza_budynkiem, what)
        } else {
            stringResource(R.string.mapa_bariera_w_budynku, what, symbol)
        }
    val isUnknownFloorOnPlan =
        planBuilding != null && symbol == planBuilding && barrier.floor?.floorId == null
    return if (isUnknownFloorOnPlan) {
        stringResource(R.string.mapa_bariera_pietro_nieznane, place)
    } else {
        place
    }
}

/** Podpis bariery [id] z jej typem, jeśli bariera jest wśród [this]. */
@Composable
internal fun ShownBarriers.caption(id: String?): String? =
    barriers.firstOrNull { it.id == id }?.let {
        stringResource(it.type?.nameRes() ?: R.string.mapa_bariera_bez_typu)
    }
