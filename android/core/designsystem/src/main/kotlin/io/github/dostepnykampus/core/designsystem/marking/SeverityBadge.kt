/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem.marking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.designsystem.MapColors
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.core.model.submission.Severity

/**
 * Uciążliwość [severity] poza mapą: znak z mapy ([SeverityIcon]) w barwach z [mapColors] i pogrubione słowo obok,
 * bez tła i ramki.
 *
 * Wymagania: NFR-21, US2.1
 *
 * @param severity uciążliwość wyświetlana jako znak i nazwa
 * @param modifier modyfikator wiersza
 * @param mapColors paleta znaków mapy; domyślnie paleta bieżącego motywu
 */
@Composable
public fun SeverityBadge(
    severity: Severity,
    modifier: Modifier = Modifier,
    mapColors: MapColors = DostepnyKampusTheme.mapColors,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Odstepy.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SeverityIcon(severity = severity, mapColors = mapColors)
        Text(
            text = stringResource(severity.nameRes()),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = Typografia.wyroznienieWaga,
        )
    }
}
