/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.geo.BuildingHint
import io.github.dostepnykampus.core.model.Building
import kotlin.math.roundToInt

/**
 * Budynki do sekcji „Wybierz budynek”; opakowanie listy, żeby Compose wiedział, że się nie zmienia.
 *
 * @property items budynki najbliżej punktu, od najbliższego, z odległością
 */
@Immutable
data class Buildings(val items: List<BuildingHint>)

/**
 * Czynności wyboru budynku na C1 (US6.2).
 *
 * @property onConfirm „Potwierdź”
 * @property onChoose „Wybierz inny” z symbolem budynku z listy
 * @property onOutside „Poza budynkiem”
 */
@Immutable
data class PlaceActions(val onConfirm: () -> Unit, val onChoose: (String) -> Unit, val onOutside: () -> Unit) {
    /** Brak czynności, np. w podglądzie stanu bez modelu widoku. */
    companion object {
        /** Czynności, które nic nie robią. */
        val None = PlaceActions(onConfirm = {}, onChoose = {}, onOutside = {})
    }
}

/**
 * Budynek zgłoszenia po pomiarze: „Czy to budynek C-13?” z nazwą i odległością, „Potwierdź”, „Wybierz inny”,
 * „Poza budynkiem” (US6.2, BR-G2). Daleko od budynków od razu „poza budynkiem”, bez komunikatu o błędzie (BR-G4).
 * Lista budynków do wyboru ma „Poza budynkiem” na końcu, jak w makiecie wyboru budynku C1 (US6.2, BR-G2).
 *
 * @param place stan wyboru
 * @param buildings budynki do wyboru z listy
 * @param actions czynności wyboru
 * @param modifier wymiary nadane przez ekran
 */
@Composable
internal fun PlaceSection(
    place: PlaceChoice,
    buildings: Buildings,
    actions: PlaceActions,
    modifier: Modifier = Modifier,
) {
    var isPicking by rememberSaveable { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        if (isPicking) {
            NearestSection(
                buildings = buildings,
                onPick = { symbol ->
                    isPicking = false
                    actions.onChoose(symbol)
                },
                onOutside = {
                    isPicking = false
                    actions.onOutside()
                },
                onCancel = { isPicking = false },
            )
        } else {
            PlaceQuestion(place = place, actions = actions, onPick = { isPicking = true })
        }
    }
}

@Composable
private fun PlaceQuestion(place: PlaceChoice, actions: PlaceActions, onPick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        when (place) {
            PlaceChoice.Locating -> {
                Text(stringResource(R.string.budynek_szukanie))
            }

            is PlaceChoice.Suggested -> {
                Hint(place.hint)
                Button(onClick = actions.onConfirm) { Text(stringResource(R.string.budynek_potwierdz)) }
                OutlinedButton(onClick = onPick) { Text(stringResource(R.string.budynek_wybierz_inny)) }
                TextButton(onClick = actions.onOutside) { Text(stringResource(R.string.budynek_poza)) }
            }

            is PlaceChoice.Chosen -> {
                Text(stringResource(R.string.budynek_wybrany, place.building.label()))
                OutlinedButton(onClick = onPick) { Text(stringResource(R.string.budynek_wybierz_inny)) }
                TextButton(onClick = actions.onOutside) { Text(stringResource(R.string.budynek_poza)) }
            }

            PlaceChoice.Outside -> {
                Text(stringResource(R.string.budynek_miejsce_poza))
                OutlinedButton(onClick = onPick) { Text(stringResource(R.string.budynek_wybierz)) }
            }
        }
    }
}

@Composable
private fun Hint(hint: BuildingHint) {
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        Text(
            text = stringResource(R.string.budynek_pytanie, hint.building.symbol),
            style = MaterialTheme.typography.titleMedium,
        )
        hint.building.name?.let { Text(it) }
        Text(
            if (hint.isInside) {
                stringResource(R.string.budynek_w_obrysie)
            } else {
                stringResource(R.string.budynek_odleglosc, hint.distanceMeters.roundToInt())
            },
        )
    }
}

@Composable
private fun NearestSection(
    buildings: Buildings,
    onPick: (String) -> Unit,
    onOutside: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        Text(
            text = stringResource(R.string.budynek_wybierz),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
        )
        Text(stringResource(R.string.budynek_najblizej))
        buildings.items.forEach { hint ->
            val meters = hint.distanceMeters.roundToInt()
            OutlinedButton(onClick = { onPick(hint.building.symbol) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.budynek_na_liscie, hint.building.label(), meters))
            }
        }
        OutlinedButton(onClick = onOutside, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.budynek_poza))
        }
        TextButton(onClick = onCancel) { Text(stringResource(R.string.budynek_anuluj)) }
    }
}

private fun Building.label(): String = name?.let { "$symbol $it" } ?: symbol
