/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem.marking

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.dostepnykampus.core.designsystem.R
import io.github.dostepnykampus.core.designsystem.campusDay
import io.github.dostepnykampus.core.model.building.AssessmentSource
import io.github.dostepnykampus.core.model.building.EntranceAssessment

/**
 * Napis o dniu i źródle oznaczenia wejścia [assessment] (PW-3).
 *
 * Wymagania: US17.3, BR-24
 *
 * @param assessment dzień i źródło oznaczenia
 * @param modifier modyfikator zewnętrzny
 */
@Composable
public fun AssessmentLine(assessment: EntranceAssessment, modifier: Modifier = Modifier) {
    Text(
        text = assessmentText(assessment),
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Dzień i źródło oznaczenia wejścia [assessment] słowami (PW-3). Wymagania: US17.3, BR-24, BR-27 */
@Composable
@ReadOnlyComposable
public fun assessmentText(assessment: EntranceAssessment): String {
    val day = campusDay(assessment.date)
    return when (assessment.source) {
        AssessmentSource.IMPORT -> stringResource(R.string.ocena_wejscia_import, day)
        AssessmentSource.PWR_PLANS -> stringResource(R.string.ocena_wejscia_plany_pwr, day)
        AssessmentSource.WALKTHROUGH -> stringResource(R.string.ocena_wejscia_obchod, day)
    }
}
