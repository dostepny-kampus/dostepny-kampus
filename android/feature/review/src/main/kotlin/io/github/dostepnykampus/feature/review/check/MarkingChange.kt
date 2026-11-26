/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.review.check

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.marking.AssessmentLine
import io.github.dostepnykampus.core.designsystem.marking.MarkingSign
import io.github.dostepnykampus.core.designsystem.pathIcon
import io.github.dostepnykampus.core.designsystem.submission.StatusLine
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.feature.review.R

private val LinkIntact: ImageVector =
    pathIcon(
        "M3.9 12c0-1.71 1.39-3.1 3.1-3.1h4V7H7c-2.76 0-5 2.24-5 5s2.24 5 5 5h4v-1.9H7c-1.71 0-3.1-1.39-3.1-3.1z" +
            "M8 13h8v-2H8v2zm9-6h-4v1.9h4c1.71 0 3.1 1.39 3.1 3.1s-1.39 3.1-3.1 3.1h-4V17h4" +
            "c2.76 0 5-2.24 5-5s-2.24-5-5-5z",
    )

private val LinkBroken: ImageVector =
    pathIcon(
        "M17 7h-4v1.9h4c1.71 0 3.1 1.39 3.1 3.1 0 1.43-.98 2.63-2.31 2.98l1.46 1.46C20.88 15.61 22 13.95 22 12" +
            "c0-2.76-2.24-5-5-5zm-1 4h-2.19l2 2H16zM2 4.27l3.11 3.11C3.29 8.12 2 9.91 2 12c0 2.76 2.24 5 5 5h4v-1.9H7" +
            "c-1.71 0-3.1-1.39-3.1-3.1 0-1.59 1.21-2.9 2.76-3.07L8.73 11H8v2h2.73L13 15.27V17h1.73l4.01 4L20 19.74" +
            " 3.27 3 2 4.27z",
    )

/**
 * Zmiana oznaczenia wejścia: obecne oznaczenie z dniem i źródłem, czy zmieniło się od wysłania, i wybrane oznaczenie.
 *
 * Wymagania: US17.2, US10.8, BR-23, BR-24, NFR-21
 *
 * @param current obecne oznaczenie; `null`, gdy serwer nie podał wejścia
 * @param chosen oznaczenie wybrane w zmianie; `null` bez oznaczenia
 */
@Composable
internal fun MarkingChange(current: CurrentMarking?, chosen: EntranceMarking?) {
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.xs)) {
        if (current != null) {
            Text(stringResource(R.string.sprawdzenie_obecne_oznaczenie), style = MaterialTheme.typography.bodyLarge)
            MarkingSign(marking = current.entrance.marking)
            current.entrance.assessment?.let { AssessmentLine(assessment = it) }
            if (current.isChangedSinceSent) {
                StatusLine(text = stringResource(R.string.sprawdzenie_zmienione_po_wyslaniu), icon = LinkBroken)
            } else {
                StatusLine(text = stringResource(R.string.sprawdzenie_bez_zmian_od_wyslania), icon = LinkIntact)
            }
        }
        if (chosen != null) {
            Text(stringResource(R.string.sprawdzenie_wybrane_oznaczenie), style = MaterialTheme.typography.bodyLarge)
            MarkingSign(marking = chosen)
        }
    }
}
