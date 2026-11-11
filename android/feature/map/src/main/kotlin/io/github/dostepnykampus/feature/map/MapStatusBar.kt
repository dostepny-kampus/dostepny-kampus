/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag

/** Znacznik tła paska stanu nad mapą dla testów. */
const val MAP_STATUS_BAR_TAG = "map-status-bar"

/**
 * Tło systemowego paska stanu nad mapą, gdy nagłówka V1 nie ma, bo mapę przykrywa arkusz. Ma barwę nagłówka, więc
 * zegar i ikony paska nie leżą na podkładzie mapy i zachowują kontrast w motywie jasnym i ciemnym (NFR-16).
 * Wysokość to górny odstęp [insets]; bez [isShown] tła nie ma.
 */
@Composable
internal fun MapStatusBar(insets: WindowInsets, isShown: Boolean, modifier: Modifier = Modifier) {
    if (!isShown) return
    val height = with(LocalDensity.current) { insets.getTop(this).toDp() }
    Spacer(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(MaterialTheme.colorScheme.surface)
            .testTag(MAP_STATUS_BAR_TAG),
    )
}
