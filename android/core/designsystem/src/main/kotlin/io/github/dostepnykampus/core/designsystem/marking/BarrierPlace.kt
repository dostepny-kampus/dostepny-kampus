/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem.marking

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import io.github.dostepnykampus.core.designsystem.R
import io.github.dostepnykampus.core.model.submission.SubmissionFloor

/**
 * Miejsce bariery słowami: „A-1 Gmach Główny · Wejście Cesarskie, parter”. Budynek bez nazwy to „Budynek C-13”,
 * a bariera poza budynkiem ma „Poza budynkiem”. Piętro budynku podstawowego to „parter” albo „piętro 2”,
 * z dopiskiem „półpiętro”.
 *
 * Wymagania: US1.5, BR-G2
 *
 * @param symbol symbol budynku; `null` to bariera poza budynkiem
 * @param name nazwa budynku z danych; `null`, gdy budynek jej nie ma
 * @param entrance nazwa wejścia; `null`, gdy zgłoszenie go nie wskazuje
 * @param floor piętro zgłoszenia; `null`, gdy zgłoszenie go nie wskazuje
 */
@Composable
@ReadOnlyComposable
public fun barrierPlace(symbol: String?, name: String?, entrance: String?, floor: SubmissionFloor?): String {
    val where =
        when {
            symbol == null -> stringResource(R.string.miejsce_poza)
            name == null -> stringResource(R.string.miejsce_budynek, symbol)
            else -> stringResource(R.string.miejsce_budynek_z_nazwa, symbol, name)
        }
    val inside = listOfNotNull(entrance, floor?.let { floorText(it) })
    return if (inside.isEmpty()) where else where + " · " + inside.joinToString(separator = ", ")
}

@Composable
@ReadOnlyComposable
private fun floorText(floor: SubmissionFloor): String? {
    val number = floor.number
    val level =
        when (number) {
            null -> null
            0 -> stringResource(R.string.miejsce_parter)
            else -> stringResource(R.string.miejsce_pietro, number)
        }
    val mezzanine = stringResource(R.string.miejsce_polpietro)
    return if (level != null && floor.isMezzanine == true) "$level, $mezzanine" else level
}
