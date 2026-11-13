/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.dostepnykampus.core.model.BuildingCatalog

/**
 * Arkusz bariery o identyfikatorze [id] po dotknięciu jej znaku na mapie V1. Budynek i nazwę wejścia
 * bierze z [catalog]. Bez [id] i wtedy, gdy bariery nie ma już wśród [barriers], np. przestała być przyjęta,
 * arkusza nie ma. Systemowe „Wstecz” przy otwartym arkuszu zamyka go i zostawia V1, jak „Zamknij”.
 *
 * Wymagania: US1.5
 *
 * @param id identyfikator zgłoszenia dotkniętego znaku; `null` bez wybranej bariery
 * @param barriers bariery pokazane na mapie
 * @param catalog katalog budynków i wejść; `null` przed jego wczytaniem
 * @param onClose zamyka arkusz: „Zamknij” zostawia samą mapę, a „Pokaż opis budynku” zamyka go przed V2
 * @param onBuildingClick „Pokaż opis budynku” otwiera V2 budynku o podanym symbolu
 * @param modifier położenie arkusza na mapie
 */
@Composable
internal fun ChosenBarrierSheet(
    id: String?,
    barriers: ShownBarriers,
    catalog: BuildingCatalog?,
    onClose: () -> Unit,
    onBuildingClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val chosen = id?.let { barriers.chosen(it, catalog) }
    BackHandler(enabled = chosen != null, onBack = onClose)
    if (chosen != null) {
        BarrierSheet(
            chosen = chosen,
            onClose = onClose,
            onBuildingClick = { building ->
                onClose()
                onBuildingClick(building.symbol)
            },
            modifier = modifier,
        )
    }
}

private fun ShownBarriers.chosen(id: String, catalog: BuildingCatalog?): ChosenBarrier? {
    val buildings = catalog?.buildings.orEmpty()
    val entrances = catalog?.entrances.orEmpty()
    return barriers.firstOrNull { it.id == id }?.let { barrier ->
        ChosenBarrier(
            barrier = barrier,
            building = buildings.firstOrNull { it.symbol == barrier.buildingSymbol },
            entrance = entrances.firstOrNull { it.id == barrier.entranceId }?.name,
        )
    }
}
