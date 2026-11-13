/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.report

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.designsystem.Obramowania
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.core.designsystem.descriptionRes
import io.github.dostepnykampus.core.designsystem.nameRes
import io.github.dostepnykampus.core.model.Severity
import io.github.dostepnykampus.feature.capture.R
import io.github.dostepnykampus.feature.capture.measure.place
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
    val name = nearest.entrance.name
    val entrance =
        when {
            name != null -> stringResource(R.string.formularz_wejscie_nazwa, name)
            nearest.entrance.isMain -> stringResource(R.string.formularz_wejscie_glowne)
            else -> stringResource(R.string.formularz_wejscie)
        }
    val meters = nearest.meters.roundToInt().coerceAtLeast(1)
    return stringResource(R.string.formularz_miejsce, meters, entrance, accuracy)
}

/**
 * „Uciążliwość”: trzy wybory z opisem, oceniane dla osoby, dla której bariera jest najtrudniejsza (PW-13).
 *
 * Wymagania: US7.1
 */
@Composable
internal fun SeveritySection(severity: Severity?, onEdit: (ReportEdit) -> Unit) {
    Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        SectionTitle(stringResource(R.string.formularz_uciazliwosc))
        Text(stringResource(R.string.formularz_uciazliwosc_podpowiedz))
        Severity.entries.forEach { option ->
            SeverityOption(option = option, isChosen = option == severity, onEdit = onEdit)
        }
    }
}

@Composable
private fun SeverityOption(option: Severity, isChosen: Boolean, onEdit: (ReportEdit) -> Unit) {
    OutlinedCard(
        modifier =
            Modifier.fillMaxWidth().selectable(selected = isChosen, role = Role.RadioButton) {
                onEdit(ReportEdit.Rating(option))
            },
        border =
            BorderStroke(
                width = if (isChosen) Obramowania.wybrane else Obramowania.ramka,
                color = MaterialTheme.colorScheme.outline,
            ),
    ) {
        Column(modifier = Modifier.padding(Odstepy.m)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = isChosen, onClick = null)
                Text(
                    text = stringResource(option.nameRes()),
                    modifier = Modifier.padding(start = Odstepy.s),
                    fontWeight = Typografia.wyroznienieWaga,
                )
            }
            Text(stringResource(option.descriptionRes()))
        }
    }
}

@Composable
internal fun SectionTitle(text: String) {
    Text(text = text, modifier = Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
}
