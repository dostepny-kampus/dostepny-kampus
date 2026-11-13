/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.barrier

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import io.github.dostepnykampus.core.data.barriers.AcceptedBarrier
import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.SubmissionFloor
import io.github.dostepnykampus.feature.map.R

private const val PLACE_SEPARATOR = " · "

/**
 * Bariera wybrana na mapie razem z tym, czego arkusz bariery nie ma w samym zgłoszeniu.
 *
 * Wymagania: US1.5
 *
 * @property barrier bariera ze zgłoszenia przyjętego
 * @property building budynek bariery z katalogu; `null` to bariera poza budynkiem albo budynek spoza katalogu
 * @property entrance nazwa wejścia z danych; `null`, gdy zgłoszenie go nie wskazuje albo dane nie mają nazwy
 */
@Immutable
internal data class ChosenBarrier(val barrier: AcceptedBarrier, val building: Building?, val entrance: String?)

/**
 * Miejsce bariery [chosen] słowami do arkusza bariery: „A-1 Gmach Główny · Wejście Cesarskie, parter”.
 * Budynek bez nazwy to „Budynek C-13”, a bariera poza budynkiem ma „Poza budynkiem”. Piętro budynku
 * podstawowego to „parter” albo „piętro 2”, z dopiskiem „półpiętro”.
 *
 * Wymagania: US1.5, BR-G2
 */
@Composable
@ReadOnlyComposable
internal fun barrierPlace(chosen: ChosenBarrier): String {
    val symbol = chosen.barrier.buildingSymbol
    val name = chosen.building?.name
    val where =
        when {
            symbol == null -> stringResource(R.string.mapa_bariera_miejsce_poza)
            name == null -> stringResource(R.string.mapa_budynek, symbol)
            else -> stringResource(R.string.mapa_bariera_budynek_z_nazwa, symbol, name)
        }
    val inside = listOfNotNull(chosen.entrance, chosen.barrier.floor?.let { floorText(it) })
    return if (inside.isEmpty()) where else where + PLACE_SEPARATOR + inside.joinToString(separator = ", ")
}

@Composable
@ReadOnlyComposable
private fun floorText(floor: SubmissionFloor): String? {
    val number = floor.number
    val level =
        when (number) {
            null -> null
            0 -> stringResource(R.string.mapa_bariera_parter)
            else -> stringResource(R.string.mapa_bariera_pietro, number)
        }
    val mezzanine = stringResource(R.string.mapa_bariera_polpietro)
    return if (level != null && floor.isMezzanine == true) "$level, $mezzanine" else level
}
