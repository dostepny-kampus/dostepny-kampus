/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.search

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import io.github.dostepnykampus.feature.map.slideUp

/**
 * Miejsce góry V1: przy liście ([isList]) cała wysokość ekranu, a pasek odjeżdża w górę o część [barExit]
 * swojej wysokości. [onHeight] dostaje wysokość całej góry z wynikami w pikselach.
 *
 * Wymagania: US1.1, US18.1
 */
internal fun Modifier.searchBarArea(
    isList: Boolean,
    barExit: Animatable<Float, AnimationVector1D>,
    onHeight: (Int) -> Unit,
): Modifier =
    then(if (isList) Modifier.fillMaxSize() else Modifier)
        .slideUp(barExit)
        .onSizeChanged { onHeight(it.height) }
