/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.quick

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.FloorOption
import io.github.dostepnykampus.core.designsystem.FloorOptions
import io.github.dostepnykampus.core.designsystem.FloorPicker
import io.github.dostepnykampus.core.designsystem.FloorPickerLayout
import io.github.dostepnykampus.core.designsystem.Obramowania
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.model.BarrierGroup
import io.github.dostepnykampus.core.model.Floor
import io.github.dostepnykampus.feature.capture.R

private val BASIC_FLOORS = -1..10

/**
 * Odpowiedź na pytanie o piętro; oba pola `null` to „Nie wiem”.
 *
 * Wymagania: US4.5, BR-14
 *
 * @property number numer piętra w budynku podstawowym albo przy nieznanej pozycji
 * @property floor piętro z danych PWr w budynku szczegółowym
 */
data class QuickFloor(val number: Int? = null, val floor: Floor? = null)

/**
 * Piętra budynku do pytania w kroku 2, od najniższego; pusta lista to budynek podstawowy.
 *
 * Wymagania: BR-G1
 *
 * @property items piętra z danych PWr
 */
data class QuickFloors(val items: List<Floor> = emptyList())

/** Napis grupy barier na V4, prostszy niż nazwa grupy w formularzu C2 (PW-7). Wymagania: BR-14 */
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
 * Krok 2 V4: „Czego dotyczy bariera?” i 6 dużych przycisków grup na całą szerokość, bez ikon.
 * Grupa wybrana wcześniej, np. po powrocie z kroku 3, jest zaznaczona obwódką, tłem i znakiem „✓”, a TalkBack czyta
 * ją jako wybraną.
 *
 * Wymagania: US4.3, NFR-17, US4.8, NFR-21
 *
 * @param selected grupa wybrana wcześniej; `null` przy pierwszym wejściu do kroku
 * @param onGroup wybrana grupa
 */
@Composable
internal fun QuickGroupQuestion(selected: BarrierGroup? = null, onGroup: (BarrierGroup) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        Text(stringResource(R.string.szybkie_pytanie_grupa))
        BarrierGroup.entries.forEach { group ->
            BigChoice(
                text = stringResource(group.quickLabel()),
                isSelected = group == selected,
                onClick = { onGroup(group) },
            )
        }
    }
}

/**
 * Krok 2 V4, pytanie o piętro po wyborze [group]: „Nie wiem” na górze i piętra od najniższego.
 * W budynku szczegółowym etykiety z danych PWr ([floors]); w podstawowym i przy nieznanej pozycji numery od -1
 * do 10, z „Parter” zamiast 0.
 *
 * Wymagania: US4.5, BR-14
 *
 * @param group wybrana grupa, pokazana nad pytaniem
 * @param floors piętra budynku z danych PWr od najniższego
 * @param onFloor odpowiedź
 */
@Composable
internal fun QuickFloorQuestion(group: BarrierGroup, floors: QuickFloors, onFloor: (QuickFloor) -> Unit) {
    val unknown = stringResource(R.string.szybkie_nie_wiem)
    val ground = stringResource(R.string.szybkie_parter)
    val answers = quickFloorAnswers(floors = floors, unknownLabel = unknown, groundLabel = ground)
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        Text(stringResource(group.quickLabel()))
        Text(stringResource(R.string.szybkie_pytanie_pietro))
        FloorPicker(
            options = FloorOptions(answers.map { it.first }),
            selectedKey = null,
            onSelect = { option -> answers.firstOrNull { it.first == option }?.let { onFloor(it.second) } },
            layout = FloorPickerLayout.LIST,
        )
    }
}

/**
 * Opcje pytania o piętro z odpowiedzią każdej: „Nie wiem” na górze, potem piętra z danych PWr od najniższego
 * albo numery od -1 do 10 w budynku podstawowym.
 *
 * Wymagania: US4.5, BR-G1
 */
internal fun quickFloorAnswers(
    floors: QuickFloors,
    unknownLabel: String,
    groundLabel: String,
): List<Pair<FloorOption, QuickFloor>> {
    val unknown = FloorOption(key = UNKNOWN_KEY, label = unknownLabel) to QuickFloor()
    val known =
        if (floors.items.isEmpty()) {
            BASIC_FLOORS.map { number ->
                val label = if (number == 0) groundLabel else number.toString()
                FloorOption(key = "n$number", label = label) to QuickFloor(number = number)
            }
        } else {
            floors.items.map { floor -> FloorOption(key = floor.id, label = floor.label) to QuickFloor(floor = floor) }
        }
    return listOf(unknown) + known
}

private const val UNKNOWN_KEY = "?"
private const val SELECTED_MARK = "✓"

@Composable
private fun BigChoice(text: String, onClick: () -> Unit, isSelected: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = CeleDotyku.przycisk).semantics { selected = isSelected },
        border = if (isSelected) BorderStroke(Obramowania.wybrane, colors.onPrimaryContainer) else null,
        colors =
            if (isSelected) {
                ButtonDefaults.outlinedButtonColors(
                    containerColor = colors.primaryContainer,
                    contentColor = colors.onPrimaryContainer,
                )
            } else {
                ButtonDefaults.outlinedButtonColors()
            },
    ) {
        Text(if (isSelected) "$SELECTED_MARK $text" else text)
    }
}
