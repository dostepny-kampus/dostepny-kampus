/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem.bar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import io.github.dostepnykampus.core.designsystem.LARGE_FONT_SCALE

private const val FIXED_HEIGHT_FRACTION = 0.25f

/**
 * Mierzy [header] i [actions] i pozostawia je poza przewijaniem tylko w budżecie 25% wysokości bieżącego okna, po
 * odjęciu miejsca zajętego poza układem. Przy dużej czcionce albo przekroczeniu budżetu przewija je razem z [content].
 * Treść nie może mieć własnego przewijania pionowego. [modifier] powinien uwzględniać wcięcia systemowe. Zmiana
 * [contentKey] rozpoczyna przewijanie nowej treści od początku.
 *
 * Wymagania: NFR-52
 */
@Composable
public fun AdaptiveScreen(
    header: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    actions: (@Composable () -> Unit)? = null,
    contentKey: Any? = Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val windowHeight = LocalWindowInfo.current.containerSize.height
    val isLarge = LocalDensity.current.fontScale >= LARGE_FONT_SCALE
    var headerHeight by remember { mutableIntStateOf(0) }
    var actionsHeight by remember { mutableIntStateOf(0) }
    val scroll = key(contentKey) { rememberScrollState() }
    val currentHeader by rememberUpdatedState(header)
    val currentActions by rememberUpdatedState(actions)
    val top = remember {
        movableContentOf {
            Box(Modifier.fillMaxWidth().onSizeChanged { headerHeight = it.height }) { currentHeader() }
        }
    }
    val bottom = remember {
        movableContentOf {
            Box(Modifier.fillMaxWidth().onSizeChanged { actionsHeight = it.height }) { currentActions?.invoke() }
        }
    }
    BoxWithConstraints(modifier.fillMaxSize()) {
        val outside = (windowHeight - constraints.maxHeight).coerceAtLeast(0)
        val budget = (windowHeight * FIXED_HEIGHT_FRACTION - outside).coerceAtLeast(0f)
        val isPinned = !isLarge && headerHeight > 0 && headerHeight + actionsHeight <= budget
        Column {
            if (isPinned) top()
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll)) {
                if (!isPinned) top()
                content()
                if (!isPinned) bottom()
            }
            if (isPinned) bottom()
        }
    }
}
