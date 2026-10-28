/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp

/**
 * Rozmiar ikony obok napisu: [Ikony.rozmiar] liczony jak tekst w sp, więc ikona rośnie razem z napisem
 * przy większej czcionce telefonu, także przy nieliniowym skalowaniu (Android 14+, NFR-20).
 */
@Composable
@ReadOnlyComposable
internal fun iconSizeLikeText(): Dp {
    val textSize = Ikony.rozmiar.value.sp
    return with(LocalDensity.current) { textSize.toDp() }
}
