/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem.card

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.dostepnykampus.core.designsystem.Odstepy

/**
 * Panel informacyjny na całą szerokość, bez ramki: tło `surfaceContainerLowest`, kształt `shapes.large`,
 * wcięcie [Odstepy.m]. Elementy [content] układają się w kolumnie co [Odstepy.s].
 *
 * Wymagania: NFR-16, NFR-20, US5.1, US14.1, US14.2
 */
@Composable
public fun InfoPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        CardColumn(content)
    }
}
