/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.designsystem.MarkingChip
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.PinnedButton
import io.github.dostepnykampus.core.designsystem.PinnedButtonKind
import io.github.dostepnykampus.core.designsystem.Typografia

/**
 * Wiersz wejścia na karcie budynku i na liście wejść: oznaczenie, znacznik „zalecane”, nazwa, ocena i przyciski
 * (US2, US17). Jeden układ dla obu ekranów, więc wygląd i opis dla czytnika ekranu się nie rozjadą (#868).
 *
 * @param listed wejście z kolejnością i znacznikiem „zalecane”
 * @param withAssessment czy pokazać ocenę wejścia; bez niej, gdy wszystkie wejścia mają tę samą ocenę nad listą
 * @param onShowEntrance „Pokaż” na mapie; `null` ukrywa przycisk
 * @param onReportEntrance „Zgłoś” barierę wejścia; `null` ukrywa przycisk
 */
@Composable
internal fun EntranceRow(
    listed: OrderedEntrance,
    withAssessment: Boolean,
    onShowEntrance: ((String) -> Unit)?,
    onReportEntrance: ((String) -> Unit)?,
) {
    val title = listed.entrance.name ?: stringResource(R.string.karta_bez_nazwy)
    val spokenShow = stringResource(R.string.wizyta_pokaz_czytaj, title)
    val spokenReport = stringResource(R.string.karta_zglos_czytaj, title)
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.xs)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Odstepy.s),
            verticalArrangement = Arrangement.spacedBy(Odstepy.xs),
        ) {
            MarkingChip(marking = listed.entrance.marking)
            if (listed.isRecommended) {
                Text(
                    text = stringResource(R.string.karta_zalecane),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = Typografia.wyroznienieWaga,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        Text(text = title, fontWeight = Typografia.wyroznienieWaga, style = MaterialTheme.typography.titleMedium)
        if (withAssessment) listed.entrance.assessment?.let { AssessmentLine(it) }
        onShowEntrance?.let { show ->
            PinnedButton(
                text = stringResource(R.string.wizyta_pokaz),
                onClick = { show(listed.entrance.id) },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = spokenShow },
                kind = PinnedButtonKind.Tonal,
            )
        }
        onReportEntrance?.let { report ->
            PinnedButton(
                text = stringResource(R.string.karta_zglos),
                onClick = { report(listed.entrance.id) },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = spokenReport },
                kind = PinnedButtonKind.Tonal,
            )
        }
    }
}
