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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.feature.map.camera.MapMotion
import kotlin.math.roundToInt

private const val BAR_SLIDE_MILLIS = 300
private val BarSlide =
    tween<Float>(durationMillis = BAR_SLIDE_MILLIS, easing = CubicBezierEasing(a = 0.2f, b = 0f, c = 0f, d = 1f))

/**
 * Część wysokości górnego paska V1 schowana nad górną krawędzią ekranu: pasek odjeżdża w górę, gdy arkusz V2
 * zasłania mapę ([isCovered]), w czasie i po krzywej ruchu mapy z [motion], i wraca z góry po zamknięciu arkusza.
 * Bez ruchu mapy pasek rusza się w czasie `medium2` po krzywej standardowej Material.
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
 * budynków znika od razu. Zmiana pozycji paska przelicza wynik tylko przy zmianie widoczności.
 */
@Composable
internal fun Animatable<Float, AnimationVector1D>.isBarShown(isCovered: Boolean, isList: Boolean): Boolean {
    val isOnScreen by remember(this) { derivedStateOf { value < 1f } }
    return !isCovered || (isOnScreen && !isList)
}

/** Góra mapy zasłonięta paskiem wysokim na [bar]; z arkuszem V2 pasek odjechał, więc nic. */
internal fun mapTop(isCovered: Boolean, bar: Dp): Dp = if (isCovered) 0.dp else bar

/** Przesuwa element w górę o część [hidden] jego wysokości; odczyt w fazie układu, bez ponownej kompozycji. */
internal fun Modifier.slideUp(hidden: Animatable<Float, AnimationVector1D>): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(width = placeable.width, height = placeable.height) {
            placeable.place(x = 0, y = -(hidden.value * placeable.height).roundToInt())
        }
    }
