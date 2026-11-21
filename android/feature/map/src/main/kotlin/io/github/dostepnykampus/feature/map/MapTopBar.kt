/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.designsystem.LARGE_FONT_SCALE
import io.github.dostepnykampus.feature.map.camera.MapMotion
import kotlin.math.roundToInt

private const val BAR_SLIDE_MILLIS = 300
private val BarSlide =
    tween<Float>(durationMillis = BAR_SLIDE_MILLIS, easing = CubicBezierEasing(a = 0.2f, b = 0f, c = 0f, d = 1f))

/**
 * Część wysokości górnego paska V1 schowana nad górną krawędzią ekranu: pasek odjeżdża w górę, gdy arkusz V2
 * albo karta sali zasłania mapę ([isCovered]), w czasie i po krzywej ruchu mapy z [motion], i wraca z góry
 * po zamknięciu arkusza albo karty. Bez ruchu mapy pasek rusza się w czasie `medium2` po krzywej standardowej Material.
 *
 * Wymagania: NFR-56, US2.8
 */
@Composable
internal fun rememberTopBarExit(isCovered: Boolean, motion: MapMotion?): Animatable<Float, AnimationVector1D> {
    val hidden = remember { Animatable(if (isCovered) 1f else 0f) }
    val after = remember(isCovered) { motion?.moves ?: 0 }
    LaunchedEffect(isCovered) {
        val target = if (isCovered) 1f else 0f
        if (isCovered && motion != null) {
            motion.slide(value = hidden, target = target, after = after, fallback = BarSlide)
        } else {
            hidden.animateTo(targetValue = target, animationSpec = BarSlide)
        }
    }
    return hidden
}

/**
 * Pasek widać bez arkusza V2, a nad arkuszem do odjazdu w całości, także w czasie czekania na ruch mapy; nad listą
 * budynków znika od razu. Pod arkuszem na cały ekran ([isUnderFullSheet]) paska nie ma. Zmiana pozycji paska
 * przelicza wynik tylko przy zmianie widoczności.
 */
@Composable
internal fun Animatable<Float, AnimationVector1D>.isBarShown(
    isCovered: Boolean,
    isList: Boolean,
    isUnderFullSheet: Boolean = false,
): Boolean {
    val isOnScreen by remember(this) { derivedStateOf { value < 1f } }
    return !isUnderFullSheet && (!isCovered || (isOnScreen && !isList))
}

/**
 * Mapę zasłania arkusz V2 wysoki na [sheetPadding] albo, przy zwykłej czcionce, karta sali pod mapą
 * ([isRoomOpen]); wtedy górny pasek V1 odjeżdża w górę, a mapa sięga górnej krawędzi. Przy dużej czcionce
 * karta sali stoi na cały ekran.
 *
 * Wymagania: US18.1, US18.2, NFR-53
 */
@Composable
@ReadOnlyComposable
internal fun isMapCovered(sheetPadding: Dp, isRoomOpen: Boolean): Boolean =
    sheetPadding > 0.dp || (isRoomOpen && LocalDensity.current.fontScale < LARGE_FONT_SCALE)

/** Góra mapy zasłonięta paskiem wysokim na [bar] px; gdy mapę zasłania arkusz albo karta, pasek odjechał, więc nic. */
@Composable
@ReadOnlyComposable
internal fun mapTop(isCovered: Boolean, bar: Int): Dp =
    if (isCovered) 0.dp else with(LocalDensity.current) { bar.toDp() }

/** Przesuwa element w górę o część [hidden] jego wysokości; odczyt w fazie układu, bez ponownej kompozycji. */
internal fun Modifier.slideUp(hidden: Animatable<Float, AnimationVector1D>): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(width = placeable.width, height = placeable.height) {
            placeable.place(x = 0, y = -(hidden.value * placeable.height).roundToInt())
        }
    }

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
