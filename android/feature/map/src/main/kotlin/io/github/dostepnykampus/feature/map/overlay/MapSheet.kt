/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.overlay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Zaokraglenia

/**
 * Arkusz na dole mapy V1 z treścią [content] w przewijanej kolumnie, więc przy dużej czcionce treść się mieści.
 *
 * Wymagania: NFR-20
 *
 * @param modifier położenie arkusza na mapie
 * @param content treść arkusza
 */
@Composable
internal fun MapSheet(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(Zaokraglenia.duze), shadowElevation = Odstepy.s) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(Odstepy.m),
            verticalArrangement = Arrangement.spacedBy(Odstepy.s),
            content = content,
        )
    }
}
