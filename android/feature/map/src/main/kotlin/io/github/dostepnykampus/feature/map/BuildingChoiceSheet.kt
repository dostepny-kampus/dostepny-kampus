/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Zaokraglenia
import io.github.dostepnykampus.core.model.Building

/**
 * Arkusz „Który budynek?” na dole mapy po dotknięciu konturu wspólnego albo dwóch konturów w zasięgu 24 dp
 * (US2, decyzja 85). Każdy budynek to jeden przycisk na pełną szerokość, jak wynik pod polem wyszukiwania:
 * symbol, nazwa z danych, gdy jest, i „Budynek C-3”. TalkBack czyta symbol i nazwę, a czynność przycisku to
 * „otwiera opis budynku” (NFR-17, NFR-18). Przy dużej czcionce arkusz się przewija (NFR-20).
 *
 * @param onClose „Zamknij” zostawia samą mapę
 * @param modifier położenie arkusza na mapie
 * @param content przyciski [BuildingChoice] budynków trafionych dotknięciem, w kolejności podpisu konturu
 */
@Composable
internal fun BuildingChoiceSheet(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(Zaokraglenia.duze), shadowElevation = Odstepy.s) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(Odstepy.m),
            verticalArrangement = Arrangement.spacedBy(Odstepy.s),
        ) {
            SheetHeader(onClose)
            content()
        }
    }
}

// Nagłówek „Który budynek?” i obok niego obrysowany przycisk „Zamknij” (NFR-53, NFR-17).
@Composable
private fun SheetHeader(onClose: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.mapa_ktory_budynek),
            modifier = Modifier.weight(1f).semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
        )
        OutlinedButton(onClick = onClose, modifier = Modifier.heightIn(min = CeleDotyku.minimum)) {
            Text(stringResource(R.string.mapa_zamknij))
        }
    }
}

/**
 * Przycisk budynku [building] w arkuszu „Który budynek?”. Cały przycisk to jeden cel dotyku, a TalkBack czyta
 * „C-3, nazwa” i czynność „otwiera opis budynku”.
 *
 * @param building budynek z katalogu: symbol i nazwa z danych, gdy jest
 * @param onClick otwiera V2 tego budynku
 */
@Composable
internal fun BuildingChoice(building: Building, onClick: () -> Unit) {
    val action = stringResource(R.string.mapa_otwiera_opis_budynku)
    val description = listOfNotNull(building.symbol, building.name).joinToString(separator = ", ")
    val shape = RoundedCornerShape(Zaokraglenia.srednie)
    val choiceSemantics =
        Modifier.clearAndSetSemantics {
            contentDescription = description
            role = Role.Button
            onClick(label = action) {
                onClick()
                true
            }
        }
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .then(choiceSemantics)
                .clickable(onClick = onClick),
        shape = shape,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(Modifier.heightIn(min = CeleDotyku.minimum).padding(Odstepy.m)) {
            Text(text = building.symbol, style = MaterialTheme.typography.titleMedium)
            building.name?.let { Text(text = it, style = MaterialTheme.typography.bodyLarge) }
            Text(stringResource(R.string.mapa_budynek, building.symbol), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
