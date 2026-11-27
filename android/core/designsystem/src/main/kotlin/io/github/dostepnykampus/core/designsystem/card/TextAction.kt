/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem.card

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.dostepnykampus.core.designsystem.CeleDotyku

/**
 * Przycisk tekstowy z treścią [content] w wierszu i celem dotyku co najmniej [CeleDotyku.minimum]; rośnie z tekstem.
 * Przy [enabled] fałszywym przycisk nie reaguje.
 *
 * Wymagania: NFR-17, NFR-20
 */
@Composable
public fun TextAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = CeleDotyku.minimum),
        enabled = enabled,
        content = content,
    )
}
