/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember

/** Wartość [value] z poprzedniej udanej kompozycji; w pierwszej kompozycji sama [value]. */
@Composable
internal fun <T> rememberPrevious(value: T): T {
    val previous = remember { PreviousValue(value) }
    val before = previous.last
    SideEffect { previous.keep(value) }
    return before
}

private class PreviousValue<T>(initial: T) {
    var last: T = initial
        private set

    fun keep(current: T) {
        last = current
    }
}
