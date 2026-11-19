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
 * Opis położenia z opisem dojścia i szerokość drzwi wejścia [shown], każde w swojej części z nagłówkiem. Puste
 * opisy i szerokość 0 nic nie dodają; bez nich kolumna jest pusta.
 *
 * Wymagania: US2.1, US3.3, BR-27
 */
@Composable
internal fun EntranceDetails(shown: ShownEntrance) {
    val entrance = shown.entrance
    val approach =
        listOf(entrance.placeDescription, entrance.approach).filter { it.isNotEmpty() }.joinToString(separator = "\n")
            .takeIf { it.isNotEmpty() }
    val door = doorWidthText(shown.entrance)
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        approach?.let { SheetPart(title = stringResource(R.string.mapa_wejscie_dojscie), text = it) }
        door?.let { SheetPart(title = stringResource(R.string.mapa_wejscie_drzwi), text = it) }
    }
}

/** Czy wejście ma opis położenia, opis dojścia albo szerokość drzwi. Wymagania: US2.1, BR-27 */
internal fun Entrance.hasDetails(): Boolean = placeDescription.isNotEmpty() || approach.isNotEmpty() || doorWidthCm > 0

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
private fun SheetPart(title: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        SheetHeading(title)
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}
