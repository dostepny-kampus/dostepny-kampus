/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.overlay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Zaokraglenia

/**
 * Arkusz na dole mapy V1 z treścią [content] w przewijanej kolumnie, więc przy dużej czcionce treść się mieści.
 *
 * Wymagania: NFR-20
 *
 * @param modifier położenie arkusza na mapie
 * @param isFull arkusz zajmuje cały ekran, bez zaokrągleń i cienia, z treścią poza paskami systemu
 * @param content treść arkusza
 */
@Composable
internal fun MapSheet(
    modifier: Modifier = Modifier,
    isFull: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = if (isFull) modifier.fillMaxSize() else modifier.fillMaxWidth(),
        shape = if (isFull) RectangleShape else RoundedCornerShape(Zaokraglenia.duze),
        shadowElevation = if (isFull) 0.dp else Odstepy.s,
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .then(if (isFull) Modifier.safeDrawingPadding() else Modifier)
                .padding(Odstepy.m),
            verticalArrangement = Arrangement.spacedBy(Odstepy.s),
            content = content,
        )
    }
}
