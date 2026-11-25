/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.report.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.core.model.building.EntranceDistance
import io.github.dostepnykampus.feature.capture.R
import java.util.Locale
import kotlin.math.roundToInt

private val POLISH = Locale.forLanguageTag("pl-PL")

/**
 * Budynek i miejsce słowami z C1 bez współrzędnych oraz „Zmień miejsce”. Miejsce brzmi np. „ok. 3 m
 * od wejścia głównego, dokładność 0,8 m”; bez wejść w danych jest sama dokładność.
 *
 * Wymagania: US7.4, NFR-19, US6.1
 */
@Composable
internal fun PlaceCard(place: ReportPlace, onChangePlace: () -> Unit) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Odstepy.m), verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
            Text(
                text = buildingText(place),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = Typografia.wyroznienieWaga,
            )
            Text(placeInWords(place))
            OutlinedButton(onClick = onChangePlace, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.formularz_zmien_miejsce))
            }
        }
    }
}

@Composable
@ReadOnlyComposable
private fun buildingText(place: ReportPlace): String {
    val symbol = place.buildingSymbol ?: return stringResource(R.string.budynek_poza)
    val name = place.buildingName
    return if (name == null) {
        stringResource(R.string.formularz_budynek, symbol)
    } else {
        stringResource(R.string.formularz_budynek_nazwa, symbol, name)
    }
}

@Composable
@ReadOnlyComposable
private fun placeInWords(place: ReportPlace): String {
    val accuracy = String.format(POLISH, "%.1f", place.accuracyMeters)
    val nearest = place.nearestEntrance ?: return stringResource(R.string.formularz_dokladnosc, accuracy)
    return stringResource(R.string.formularz_miejsce, nearestInWords(nearest), accuracy)
}

/**
 * Odległość od najbliższego wejścia słowami, np. „ok. 3 m od wejścia głównego”; bez współrzędnych.
 *
 * Wymagania: US7.4, US8.4, NFR-19
 */
@Composable
@ReadOnlyComposable
internal fun nearestInWords(nearest: EntranceDistance): String {
    val name = nearest.entrance.name
    val entrance =
        when {
            name != null && nearest.entrance.hasName -> stringResource(R.string.formularz_wejscie_nazwa, name)
            nearest.entrance.isMain -> stringResource(R.string.formularz_wejscie_glowne)
            else -> stringResource(R.string.formularz_wejscie)
        }
    val meters = nearest.meters.roundToInt().coerceAtLeast(1)
    return stringResource(R.string.formularz_miejsce_od, meters, entrance)
}

@Composable
internal fun SectionTitle(text: String) {
    Text(text = text, modifier = Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
}
