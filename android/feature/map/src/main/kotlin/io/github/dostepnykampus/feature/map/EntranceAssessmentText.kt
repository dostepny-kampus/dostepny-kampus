/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.dostepnykampus.core.model.AssessmentSource
import io.github.dostepnykampus.core.model.EntranceAssessment
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DAY_FORMAT = DateTimeFormatter.ofPattern("d.M.yyyy", Locale.ROOT)

/**
 * Wspólne oznaczenie wejść [entrances] (US17.3, BR-24): dzień i źródło, gdy każde wejście ma te same.
 * `null`, gdy lista jest pusta, któreś wejście nie ma oznaczenia (z pliku) albo oznaczenia się różnią — wtedy
 * napis stoi przy każdym wejściu.
 */
internal fun commonAssessment(entrances: List<OrderedEntrance>): EntranceAssessment? =
    entrances.map { it.entrance.assessment }.distinct().singleOrNull()

/** Napis o dniu i źródle oznaczenia (US17.3, BR-24, PW-3); bez oznaczenia (wejście z pliku) nic nie pokazuje. */
@Composable
internal fun AssessmentLine(assessment: EntranceAssessment?) {
    if (assessment == null) return
    val day = DAY_FORMAT.format(assessment.date)
    val text =
        when (assessment.source) {
            AssessmentSource.IMPORT -> stringResource(R.string.karta_ocena_import, day)
            AssessmentSource.WALKTHROUGH -> stringResource(R.string.karta_ocena_obchod, day)
        }
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
