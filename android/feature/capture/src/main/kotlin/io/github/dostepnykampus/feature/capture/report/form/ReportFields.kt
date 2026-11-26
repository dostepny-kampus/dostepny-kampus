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
import io.github.dostepnykampus.core.designsystem.card.OutlinedAction
import io.github.dostepnykampus.core.designsystem.marking.measuredPlace
import io.github.dostepnykampus.core.designsystem.marking.nearEntrance
import io.github.dostepnykampus.feature.capture.R

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
            OutlinedAction(onClick = onChangePlace, modifier = Modifier.fillMaxWidth()) {
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
    val near = place.nearestEntrance?.let { nearEntrance(entrance = it.entrance, meters = it.meters) }
    return measuredPlace(near = near, accuracyMeters = place.accuracyMeters)
}

@Composable
internal fun SectionTitle(text: String) {
    Text(text = text, modifier = Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
}

/**
 * Etykieta piętra budynku podstawowego jak w danych PWr: -1 to „Piwnica”, 0 to „Parter”, pozostałe to
 * „N piętro”.
 *
 * Wymagania: US4.5, BR-G1
 */
@Composable
@ReadOnlyComposable
internal fun basicFloorLabel(floor: Int): String =
    when (floor) {
        -1 -> stringResource(R.string.szybkie_piwnica)
        0 -> stringResource(R.string.szybkie_parter)
        else -> stringResource(R.string.szybkie_pietro, floor)
    }
