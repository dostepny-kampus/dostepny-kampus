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
import io.github.dostepnykampus.core.model.Floor

private val BASIC_FLOORS = -1..10

/**
 * Odpowiedź na pytanie o piętro (US4.5, BR-14); oba pola `null` to „Nie wiem”.
 *
 * @property number numer piętra w budynku podstawowym albo przy nieznanej pozycji
 * @property floor piętro z danych PWr w budynku szczegółowym
 */
data class QuickFloor(val number: Int? = null, val floor: Floor? = null)

/**
 * Piętra budynku do pytania w kroku 2, od najniższego; pusta lista to budynek podstawowy (BR-G1).
 *
 * @property items piętra z danych PWr
 */
data class QuickFloors(val items: List<Floor> = emptyList())

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
 * Krok 2 V4, pytanie o piętro po wyborze [group]: „Nie wiem” na górze i piętra od najniższego (US4.5, BR-14).
 * W budynku szczegółowym etykiety z danych PWr ([floors]); w podstawowym i przy nieznanej pozycji numery od -1
 * do 10, z „Parter” zamiast 0.
 *
 * @param group wybrana grupa, pokazana nad pytaniem
 * @param floors piętra budynku z danych PWr od najniższego
 * @param onFloor odpowiedź
 */
@Composable
internal fun QuickFloorQuestion(group: BarrierGroup, floors: QuickFloors, onFloor: (QuickFloor) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        Text(stringResource(group.quickLabel()))
        Text(stringResource(R.string.szybkie_pytanie_pietro))
        BigChoice(text = stringResource(R.string.szybkie_nie_wiem), onClick = { onFloor(QuickFloor()) })
        floors.items.forEach { floor ->
            BigChoice(text = floor.label, onClick = { onFloor(QuickFloor(floor = floor)) })
        }
        if (floors.items.isEmpty()) {
            BASIC_FLOORS.forEach { number ->
                val label = if (number == 0) stringResource(R.string.szybkie_parter) else number.toString()
                BigChoice(text = label, onClick = { onFloor(QuickFloor(number = number)) })
            }
        }
    }
}

@Composable
private fun BigChoice(text: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = CeleDotyku.przycisk)) {
        Text(text)
    }
}
