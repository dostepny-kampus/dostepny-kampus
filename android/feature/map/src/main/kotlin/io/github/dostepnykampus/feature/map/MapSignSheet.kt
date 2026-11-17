/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Modifier
import io.github.dostepnykampus.core.model.building.BuildingCatalog
import io.github.dostepnykampus.feature.map.barrier.ChosenBarrierSheet
import io.github.dostepnykampus.feature.map.barrier.ShownBarriers
import io.github.dostepnykampus.feature.map.camera.TouchSign
import io.github.dostepnykampus.feature.map.entrance.ChosenEntranceSheet

/**
 * Znak [sign] obiektu o identyfikatorze [id] dotknięty na mapie V1, z otwartym arkuszem.
 *
 * Wymagania: US1.5, US2.1
 */
internal data class ChosenSign(val sign: TouchSign, val id: String)

/** Zapis [ChosenSign] w stanie ekranu; `null` to mapa bez arkusza znaku. */
internal val chosenSignSaver: Saver<ChosenSign?, Any> =
    listSaver(
        save = { chosen -> listOfNotNull(chosen?.sign?.name, chosen?.id) },
        restore = { saved ->
            saved.takeIf { it.size == 2 }?.let { (sign, id) -> ChosenSign(sign = TouchSign.valueOf(sign), id = id) }
        },
    )

/**
 * Arkusz znaku [chosen]: bariera spośród [barriers] albo wejście z [catalog]; bez znaku arkusza nie ma.
 *
 * Wymagania: US1.5, US2.1, US3.3
 *
 * @param onClose zamyka arkusz
 * @param onBuildingClick V2 budynku o podanym symbolu
 * @param onEntrancesClick V3 budynku o podanym symbolu
 * @param modifier położenie arkusza na mapie
 */
@Composable
internal fun ChosenSignSheet(
    chosen: ChosenSign?,
    barriers: ShownBarriers,
    catalog: BuildingCatalog?,
    onClose: () -> Unit,
    onBuildingClick: (String) -> Unit,
    onEntrancesClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val id = chosen?.id
    when (chosen?.sign) {
        TouchSign.BARRIER ->
            ChosenBarrierSheet(
                id = id,
                barriers = barriers,
                catalog = catalog,
                onClose = onClose,
                onBuildingClick = onBuildingClick,
                modifier = modifier,
            )

        TouchSign.ENTRANCE ->
            ChosenEntranceSheet(
                id = id,
                catalog = catalog,
                onClose = onClose,
                onEntrancesClick = onEntrancesClick,
                modifier = modifier,
            )

        null -> Unit
    }
}
