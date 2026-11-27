/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem.card

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.designsystem.Obramowania

/**
 * Przycisk wypełniony czynności głównej w treści ekranu z treścią [content] w wierszu: wysokość co najmniej
 * [CeleDotyku.przycisk] (rośnie z tekstem), zaokrąglenie `extraLarge`. W ciemnym motywie ma tło `primaryContainer`,
 * napis `onPrimaryContainer` i ramkę [Obramowania.wypelniony] w kolorze `primary`, żeby nie świecił i odróżniał się
 * bez koloru. Przy [enabled] fałszywym przycisk nie reaguje.
 *
 * Wymagania: NFR-16, NFR-17, NFR-20, NFR-21
 */
@Composable
public fun PrimaryAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDarkTheme = DostepnyKampusTheme.isDark
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = CeleDotyku.przycisk),
        enabled = enabled,
        shape = MaterialTheme.shapes.extraLarge,
        colors =
            if (isDarkTheme) {
                ButtonDefaults.buttonColors(
                    containerColor = colorScheme.primaryContainer,
                    contentColor = colorScheme.onPrimaryContainer,
                )
            } else {
                ButtonDefaults.buttonColors()
            },
        border = if (isDarkTheme) BorderStroke(Obramowania.wypelniony, colorScheme.primary) else null,
        content = content,
    )
}
