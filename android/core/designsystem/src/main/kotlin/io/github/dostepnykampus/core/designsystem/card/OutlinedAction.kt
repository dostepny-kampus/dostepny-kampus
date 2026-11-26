/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem.card

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.Obramowania
import io.github.dostepnykampus.core.designsystem.Zaokraglenia

/**
 * Przycisk obrysowany w treści ekranu z treścią [content] w wierszu: ramka [Obramowania.ramka] w kolorze `outline`,
 * napis i ikony w kolorze `primary`, kształt [Zaokraglenia.kapsula], wysokość co najmniej [CeleDotyku.minimum]
 * i rośnie z tekstem. Szerokość i większą wysokość nadaje [modifier]; przy [enabled] fałszywym przycisk nie reaguje.
 *
 * Wymagania: NFR-16, NFR-17, NFR-20
 */
@Composable
public fun OutlinedAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = CeleDotyku.minimum),
        enabled = enabled,
        shape = RoundedCornerShape(Zaokraglenia.kapsula),
        border = BorderStroke(Obramowania.ramka, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
        content = content,
    )
}
