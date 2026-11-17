/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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

/**
 * Arkusz znaku na mapie V1: który znak ma otwarty arkusz.
 *
 * Wymagania: US1.5, US2.1
 *
 * @param initial znak otwarty od początku; `null` bez arkusza
 */
@Stable
internal class SignSheetState(initial: ChosenSign?) {
    /** Znak z otwartym arkuszem; `null` bez arkusza. */
    var chosen by mutableStateOf(initial)
        private set

    /** Identyfikator bariery z otwartym arkuszem; `null`, gdy otwarty arkusz nie dotyczy bariery. */
    val barrierId: String?
        get() = chosen?.takeIf { it.sign == TouchSign.BARRIER }?.id

    /** Otwiera arkusz znaku [sign] obiektu [id] w miejsce poprzedniego. */
    fun open(sign: TouchSign, id: String) {
        chosen = ChosenSign(sign = sign, id = id)
    }

    /** Zamyka arkusz. */
    fun close() {
        chosen = null
    }
}

private val signSheetSaver: Saver<SignSheetState, List<String>> =
    Saver(
        save = { state -> state.chosen?.run { listOf(sign.name, id) } },
        restore = { (sign, id) -> SignSheetState(ChosenSign(sign = TouchSign.valueOf(sign), id = id)) },
    )

/** Stan arkusza znaku zachowany przy zmianie konfiguracji; na początku bez arkusza. */
@Composable
internal fun rememberSignSheetState(): SignSheetState =
    rememberSaveable(saver = signSheetSaver) { SignSheetState(null) }

/**
 * Arkusz znaku z [signs]: bariera spośród [barriers] albo wejście z [catalog]; bez znaku arkusza nie ma.
 *
 * Wymagania: US1.5, US2.1, US3.3
 *
 * @param signs znak z otwartym arkuszem; zamknięcie arkusza go zdejmuje
 * @param barriers bariery pokazane na mapie
 * @param catalog katalog budynków i wejść; `null` przed jego wczytaniem
 * @param onBuildingClick V2 budynku o podanym symbolu
 * @param onEntrancesClick V3 budynku o podanym symbolu
 * @param modifier położenie arkusza na mapie
 */
@Composable
internal fun ChosenSignSheet(
    signs: SignSheetState,
    barriers: ShownBarriers,
    catalog: BuildingCatalog?,
    onBuildingClick: (String) -> Unit,
    onEntrancesClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val id = signs.chosen?.id
    when (signs.chosen?.sign) {
        TouchSign.BARRIER -> {
            ChosenBarrierSheet(
                id = id,
                barriers = barriers,
                catalog = catalog,
                onClose = signs::close,
                onBuildingClick = onBuildingClick,
                modifier = modifier,
            )
        }

        TouchSign.ENTRANCE -> {
            ChosenEntranceSheet(
                id = id,
                catalog = catalog,
                onClose = signs::close,
                onEntrancesClick = onEntrancesClick,
                modifier = modifier,
            )
        }

        null -> {}
    }
}
