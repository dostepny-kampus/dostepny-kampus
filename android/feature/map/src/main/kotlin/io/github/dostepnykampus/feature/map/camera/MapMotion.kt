/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.camera

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.MotionDurationScale
import kotlinx.coroutines.withContext

/** Najdłuższe czekanie elementu nad mapą na ruch mapy, zanim ruszy sam. */
const val MOVE_WAIT_MILLIS: Long = 150L

/**
 * Ruchy mapy V1 do wybranego budynku nad kartą V2: liczba ruchów [moves] i czas ostatniego [durationMillis].
 * Karta V2 i górny pasek ruszają się w tym samym czasie i po krzywej [Easing], co mapa.
 *
 * Wymagania: NFR-56, US2.8
 */
@Stable
class MapMotion {
    /** Liczba ruchów mapy do budynku od utworzenia; rośnie o 1 przy każdym nowym ruchu. */
    var moves: Int by mutableIntStateOf(0)
        private set

    /** Czas ostatniego ruchu do budynku w ms, już ze skalą animacji systemu; 0 to od razu stan końcowy. */
    var durationMillis: Long by mutableLongStateOf(0L)
        private set

    /** Zapis nowego ruchu mapy do budynku [move]; zapisuje go mapa przy każdym wyborze budynku nad kartą. */
    fun record(move: CameraTransition) {
        durationMillis = move.durationMillis
        moves += 1
    }

    /**
     * Przesuwa [value] do [target] razem z pierwszym ruchem mapy po ruchu numer [after] (wartość [moves] z chwili,
     * gdy element zaczął się zmieniać): w czasie tego ruchu i po [Easing]. Bez ruchu w ciągu [MOVE_WAIT_MILLIS]
     * zegara klatek przesuwa po [fallback]. Czas ruchu ma już skalę animacji systemu, więc nie jest skalowany
     * drugi raz.
     */
    suspend fun slide(
        value: Animatable<Float, AnimationVector1D>,
        target: Float,
        after: Int,
        fallback: AnimationSpec<Float>,
    ) {
        if (!awaitMove(after)) {
            value.animateTo(targetValue = target, animationSpec = fallback)
        } else {
            val spec = tween<Float>(durationMillis = durationMillis.toInt(), easing = Easing)
            withContext(MoveTimeScale) { value.animateTo(targetValue = target, animationSpec = spec) }
        }
    }

    private suspend fun awaitMove(after: Int): Boolean {
        val start = withFrameMillis { it }
        while (moves <= after) {
            if (withFrameMillis { it } - start >= MOVE_WAIT_MILLIS) return false
        }
        return true
    }

    /** Krzywa przesunięcia mapy. */
    companion object {
        /** Krzywa przesunięcia MapLibre, cubic-bezier(0; 0; 0,25; 1). */
        val Easing: Easing = CubicBezierEasing(a = 0f, b = 0f, c = 0.25f, d = 1f)
    }
}

private object MoveTimeScale : MotionDurationScale {
    override val scaleFactor: Float = 1f
}
