/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.dostepnykampus.core.model.AssessmentSource
import io.github.dostepnykampus.core.model.EntranceAssessment
import io.github.dostepnykampus.feature.map.R
import io.github.dostepnykampus.feature.map.data.DAY_FORMAT

/**
 * Wspólne oznaczenie wejść [entrances]: dzień i źródło, gdy każde wejście ma te same.
 * `null`, gdy lista jest pusta, któreś wejście nie ma oznaczenia (z pliku) albo oznaczenia się różnią — wtedy
 * napis stoi przy każdym wejściu.
 *
 * Wymagania: US17.3, BR-24
 */
internal fun commonAssessment(entrances: List<OrderedEntrance>): EntranceAssessment? =
    entrances.map { it.entrance.assessment }.distinct().singleOrNull()

/** Napis o dniu i źródle oznaczenia (PW-3). Wymagania: US17.3, BR-24 */
@Composable
internal fun AssessmentLine(assessment: EntranceAssessment) {
    val day = DAY_FORMAT.format(assessment.date)
    val text =
        when (assessment.source) {
            AssessmentSource.IMPORT -> stringResource(R.string.karta_ocena_import, day)
            AssessmentSource.WALKTHROUGH -> stringResource(R.string.karta_ocena_obchod, day)
        }
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
