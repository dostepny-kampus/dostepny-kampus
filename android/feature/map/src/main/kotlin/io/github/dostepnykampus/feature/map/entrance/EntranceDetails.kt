/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.entrance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.feature.map.R

/**
 * Opis dojścia, szerokość drzwi i winda za drzwiami wejścia [shown], każde w swojej części z nagłówkiem. Pusty opis,
 * szerokość 0 i brak windy nic nie dodają.
 *
 * Wymagania: US2.1, US3.3, US16.3, BR-25, BR-27
 */
@Composable
internal fun EntranceDetails(shown: ShownEntrance) {
    val approach = shown.entrance.approach.takeIf { it.isNotEmpty() }
    val door = doorWidthText(shown.entrance)
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        approach?.let { SheetPart(title = stringResource(R.string.mapa_wejscie_dojscie), text = it) }
        door?.let { SheetPart(title = stringResource(R.string.mapa_wejscie_drzwi), text = it) }
        shown.lift?.let { LiftPart(it) }
    }
}

/** Czy wejście ma w danych opis dojścia, szerokość drzwi albo windę. Wymagania: US2.1, US16.3, BR-27 */
internal fun ShownEntrance.hasDetails(): Boolean =
    entrance.approach.isNotEmpty() || entrance.doorWidthCm > 0 || lift != null

/** Szerokość drzwi [entrance] słowami; `null`, gdy dane jej nie podają. Wymagania: US2.1, BR-27 */
@Composable
@ReadOnlyComposable
internal fun doorWidthText(entrance: Entrance): String? =
    entrance.doorWidthCm.takeIf { it > 0 }?.let { stringResource(R.string.karta_szerokosc_drzwi, it) }

/** Nagłówek części arkusza wejścia [text], ogłaszany jako nagłówek. Wymagania: US3.3, NFR-15 */
@Composable
internal fun SheetHeading(text: String) {
    Text(
        text = text,
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.titleMedium,
        fontWeight = Typografia.wyroznienieWaga,
    )
}

@Composable
private fun LiftPart(lift: EntranceLift) {
    val range = liftRange(lift.floors)
    val stops =
        when {
            range != null -> stringResource(R.string.mapa_wejscie_winda_jezdzi, range.first, range.second)
            lift.floors.isEmpty() -> null
            else -> stringResource(R.string.mapa_wejscie_winda_jedzie, lift.floors.joinToString { it.floorWord() })
        }
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        SheetHeading(stringResource(R.string.mapa_wejscie_za_drzwiami))
        val near = stringResource(R.string.mapa_wejscie_winda, lift.meters)
        Text(text = near, style = MaterialTheme.typography.bodyLarge)
        stops?.let { Text(text = it, style = MaterialTheme.typography.bodyLarge) }
    }
}

@Composable
private fun SheetPart(title: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        SheetHeading(title)
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}
