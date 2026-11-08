/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.model.BarrierGroup

/** Piętra budynku podstawowego w pytaniu kroku 2, od parteru; budynek szczegółowy dostanie etykiety PWr (US4.5). */
private val BASIC_FLOORS = 0..4

/** Napis grupy barier na V4, prostszy niż nazwa grupy w formularzu C2 (BR-14, PW-7). */
@StringRes
internal fun BarrierGroup.quickLabel(): Int =
    when (this) {
        BarrierGroup.ENTRANCE_AND_DOORS -> R.string.szybkie_grupa_wejscie
        BarrierGroup.VERTICAL_CIRCULATION -> R.string.szybkie_grupa_schody
        BarrierGroup.HORIZONTAL_CIRCULATION -> R.string.szybkie_grupa_przejscia
        BarrierGroup.TOILETS -> R.string.szybkie_grupa_toalety
        BarrierGroup.ORIENTATION_AND_INFORMATION -> R.string.szybkie_grupa_napisy
        BarrierGroup.OTHER -> R.string.szybkie_grupa_inne
    }

/**
 * Krok 2 V4: „Czego dotyczy bariera?” i 6 dużych przycisków grup na całą szerokość, bez ikon (US4.3, NFR-17).
 *
 * @param onGroup wybrana grupa
 */
@Composable
internal fun QuickGroupQuestion(onGroup: (BarrierGroup) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        Text(stringResource(R.string.szybkie_pytanie_grupa))
        BarrierGroup.entries.forEach { group ->
            BigChoice(text = stringResource(group.quickLabel()), onClick = { onGroup(group) })
        }
    }
}

/**
 * Krok 2 V4, pytanie o piętro po wyborze [group]: „Nie wiem” i piętra od najniższego (US4.5, BR-14).
 *
 * @param group wybrana grupa, pokazana nad pytaniem
 * @param onFloor numer piętra; `null` to „Nie wiem”
 */
@Composable
internal fun QuickFloorQuestion(group: BarrierGroup, onFloor: (Int?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        Text(stringResource(group.quickLabel()))
        Text(stringResource(R.string.szybkie_pytanie_pietro))
        BigChoice(text = stringResource(R.string.szybkie_nie_wiem), onClick = { onFloor(null) })
        BASIC_FLOORS.forEach { floor ->
            val label = if (floor == 0) stringResource(R.string.szybkie_parter) else floor.toString()
            BigChoice(text = label, onClick = { onFloor(floor) })
        }
    }
}

@Composable
private fun BigChoice(text: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = CeleDotyku.przycisk)) {
        Text(text)
    }
}
