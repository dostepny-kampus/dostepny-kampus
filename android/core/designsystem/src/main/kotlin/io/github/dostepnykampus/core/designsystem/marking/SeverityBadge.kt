/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem.marking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.dostepnykampus.core.designsystem.MapColors
import io.github.dostepnykampus.core.designsystem.Obramowania
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.core.designsystem.Zaokraglenia
import io.github.dostepnykampus.core.model.submission.Severity

/**
 * Plakietka uciążliwości: znak i słowo.
 *
 * Wymagania: NFR-21
 *
 * @param severity uciążliwość wyświetlana jako znak i nazwa
 * @param modifier modyfikator plakietki
 * @param mapColors paleta znaku z mapy; `null` używa barw motywu ekranu
 */
@Composable
public fun SeverityBadge(severity: Severity, modifier: Modifier = Modifier, mapColors: MapColors? = null) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Zaokraglenia.srednie),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(Obramowania.ramka, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Odstepy.m, vertical = Odstepy.s),
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
}
