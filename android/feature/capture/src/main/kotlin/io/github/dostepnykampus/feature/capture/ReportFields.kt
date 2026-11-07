/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

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
import java.util.Locale

private val POLISH = Locale.forLanguageTag("pl-PL")

/** Budynek i dokładność z C1 bez współrzędnych oraz „Zmień miejsce” (US7.4, NFR-19). */
@Composable
internal fun PlaceCard(place: ReportPlace, onChangePlace: () -> Unit) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Odstepy.m), verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
            val building =
                place.buildingSymbol?.let { stringResource(R.string.formularz_budynek, it) }
                    ?: stringResource(R.string.budynek_poza)
            Text(text = building, style = MaterialTheme.typography.titleMedium, fontWeight = Typografia.wyroznienieWaga)
            val accuracy = String.format(POLISH, "%.1f", place.accuracyMeters)
            Text(stringResource(R.string.formularz_dokladnosc, accuracy))
            OutlinedButton(onClick = onChangePlace, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.formularz_zmien_miejsce))
            }
        }
    }
}

/** „Uciążliwość”: trzy wybory z opisem, oceniane dla osoby, dla której bariera jest najtrudniejsza (US7.1, PW-13). */
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
