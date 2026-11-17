/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.GroupContainer
import io.github.dostepnykampus.feature.map.R
import io.github.dostepnykampus.feature.map.overlay.MapControlIcons

/**
 * Opis piętra z funkcji [functions]: od najczęstszej, z liczbą w nawiasie przy kilku, kolejne od małej litery,
 * np. „Sala (2), portiernia.”.
 *
 * Wymagania: US16.3, NFR-19
 */
internal fun floorDescriptionText(functions: List<Counted>): String =
    functions
        .mapIndexed { index, function ->
            val name = if (index == 0) function.name else function.name.replaceFirstChar { it.lowercase() }
            if (function.count > 1) "$name (${function.count})" else name
        }.joinToString(postfix = ".")

/**
 * Grupa „Piętra” z faktów [facts]: piętra od najniższego, opis piętra rozwijany etykietą, na start zwinięty;
 * piętro bez opisu ma samą etykietę. Pod etykietą „Pokaż na planie” daje [onShowPlan] identyfikator piętra.
 * Bez faktów o piętrach grupa ma napis o braku planu, gdy budynek jest podstawowy ([isBasic]), a inaczej napis,
 * że plan sprawdzimy po połączeniu z internetem.
 *
 * Wymagania: US16.3, US16.2, NFR-10, NFR-17, NFR-18, NFR-19
 */
@Composable
internal fun FloorsGroup(facts: FloorFacts?, isBasic: Boolean, onShowPlan: (String) -> Unit = {}) {
    GroupContainer(title = stringResource(R.string.karta_pietra)) {
        if (facts == null) {
            val text = if (isBasic) R.string.karta_bez_planu else R.string.karta_plan_nieznany
            Text(text = stringResource(text), style = MaterialTheme.typography.bodyLarge)
        } else {
            facts.floors.forEachIndexed { index, described ->
                if (index > 0) HorizontalDivider()
                FloorRow(described = described, onShowPlan = { onShowPlan(described.floor.id) })
            }
        }
    }
}

@Composable
private fun FloorRow(described: FloorDescription, onShowPlan: () -> Unit) {
    val label = described.floor.label
    var isExpanded by rememberSaveable(described.floor.id) { mutableStateOf(false) }
    val isExpandable = described.functions.isNotEmpty()
    val state = stringResource(if (isExpanded) R.string.karta_rozwiniete else R.string.karta_zwiniete)
    val header = Modifier.fillMaxWidth().heightIn(min = CeleDotyku.minimum)
    Column {
        Row(
            modifier =
                if (isExpandable) {
                    header
                        .clickable(role = Role.Button) { isExpanded = !isExpanded }
                        .semantics { stateDescription = state }
                } else {
                    header
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            if (isExpandable) {
                Icon(if (isExpanded) MapControlIcons.Up else MapControlIcons.Down, contentDescription = null)
            }
        }
        if (isExpanded) {
            Text(text = floorDescriptionText(described.functions), style = MaterialTheme.typography.bodyLarge)
        }
        val action = stringResource(R.string.karta_pokaz_na_planie_czytaj, label)
        TextButton(onClick = onShowPlan, modifier = Modifier.semantics { contentDescription = action }) {
            Text(stringResource(R.string.karta_pokaz_na_planie))
        }
    }
}
