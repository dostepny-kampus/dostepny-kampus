/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.quick

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.dostepnykampus.core.designsystem.bar.PinnedActionScope

/**
 * Miejsce na przypięte czynności kroku V4 pod przewijaną treścią. Krok wstawia je, gdy jest na ekranie, i zdejmuje,
 * gdy znika, np. gdy zamiast niego stoi prośba o zgodę na lokalizację.
 *
 * Wymagania: NFR-52
 */
@Stable
internal class QuickPinnedSlot {
    /** Przypięte czynności bieżącego kroku; `null`, gdy krok ich nie ma. */
    var content: (@Composable PinnedActionScope.() -> Unit)? by mutableStateOf(null)
}

/**
 * Wstawia [content] w [slot], gdy krok jest na ekranie, i zdejmuje je, gdy krok znika.
 *
 * Wymagania: NFR-52
 */
@Composable
internal fun PinnedIn(slot: QuickPinnedSlot, content: @Composable PinnedActionScope.() -> Unit) {
    SideEffect { slot.content = content }
    DisposableEffect(slot) { onDispose { slot.content = null } }
}
