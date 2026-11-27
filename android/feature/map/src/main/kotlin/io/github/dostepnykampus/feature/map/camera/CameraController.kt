/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.camera

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import timber.log.Timber
import kotlin.math.abs

private const val PADDING_MILLIS = 300L
private const val RESUME_MILLIS = 300L
private const val SAME_INSET_PX = 1.0

/**
 * Kamera mapy, którą zmienia tylko [CameraController]: MapLibre w aplikacji, udawana mapa w teście.
 *
 * Wymagania: US2.8, NFR-56
 */
internal interface CameraPort {
    /** Bieżący widok kamery. */
    val camera: MapCamera

    /** Bieżące dopełnienie kamery w pikselach. */
    val inset: MapInset

    /** Zdejmuje granice kamery na czas ruchów. */
    fun lift()

    /**
     * Ruch [motion] do widoku [camera] z dopełnieniem [inset] w [millis] ms; [CameraMotion.NONE] ustawia widok
     * i dopełnienie synchronicznie. [end] dostaje koniec ruchu, także przerwanego; zgłoszenie końca nie mówi,
     * czy kamera doszła do celu.
     */
    fun move(camera: MapCamera, inset: MapInset, motion: CameraMotion, millis: Long, end: () -> Unit)

    /** Kamera stoi: granice [limits] wracają do mapy (`null` bez granic), a przelot się kończy. */
    fun rest(limits: MapLimits?)

    /** Wywołuje [block] w następnym obiegu wątku głównego, poza wywołaniem MapLibre. */
    fun post(block: () -> Unit)
}

/**
 * Ruch kamery mapy widziany przez kartę V2 i pasek górny: spoczynek albo ruch numer [Moving.number] rodzaju
 * [Moving.motion] w [Moving.durationMillis] ms.
 *
 * Wymagania: US2.8, NFR-56
 */
internal sealed interface CameraMoveState {
    /** Kamera stoi. */
    data object Idle : CameraMoveState

    /** Trwa ruch numer [number]; nowy cel daje nowy numer. */
    data class Moving(val number: Int, val motion: CameraMotion, val durationMillis: Long) : CameraMoveState
}

/**
 * Jedyne miejsce, które zmienia kamerę mapy [port]. Nowy cel przerywa trwający ruch („ostatni wygrywa”), granice
 * terenu idą do mapy tylko w spoczynku, a dopełnienie zmienione w trakcie ruchu dosuwa się po jego końcu. Koniec
 * ruchu jest obsługiwany w następnym obiegu wątku, nie w wywołaniu MapLibre. Ruch przerwany nie przez osobę ani
 * nowy cel (np. skokiem kamery w MapLibre) kończy się raz jeszcze przesunięciem do celu.
 *
 * Wymagania: US1.8, US2.8, NFR-56, BR-G5
 */
internal class CameraController(private val port: CameraPort) {
    /** Ruch kamery w tej chwili; zmiana rekomponuje czytających. */
    var state: CameraMoveState by mutableStateOf(CameraMoveState.Idle)
        private set

    private var moves = 0
    private var goal: MapCamera? = null
    private var goalInset = MapInset()
    private var limits: MapLimits? = null
    private var isResumed = false
    private var isByPerson = false

    /** Ustawia pierwszy widok [camera] z dopełnieniem [inset] synchronicznym skokiem, pozostając w spoczynku. */
    fun place(camera: MapCamera, inset: MapInset) {
        goal = camera
        goalInset = inset
        port.move(camera = camera, inset = inset, motion = CameraMotion.NONE, millis = 0L) {}
    }

    /** Ruch [move] z dopełnieniem [inset]; kamera już w celu przy tym samym dopełnieniu stoi. */
    fun move(move: CameraTransition, inset: MapInset) {
        isByPerson = false
        isResumed = false
        if (state == CameraMoveState.Idle && move.camera.isSameView(port.camera) && inset.isSameInset(port.inset)) {
            Timber.d(stayLine(port.camera))
            return
        }
        start(move = move, inset = inset)
    }

    /** Nowe dopełnienie [next]: w spoczynku dosuwa kamerę, w trakcie ruchu czeka na jego koniec. */
    fun pad(next: MapInset) {
        goalInset = next
        if (state == CameraMoveState.Idle && !next.isSameInset(port.inset)) padNow(next)
    }

    /** Nowe granice terenu [next] (`null` bez granic); w trakcie ruchu czekają na jego koniec. */
    fun limit(next: MapLimits?) {
        limits = next
        if (state == CameraMoveState.Idle) port.rest(next)
    }

    /** Osoba ruszyła mapę gestem: przerwany ruch nie wraca do celu. */
    fun byPerson() {
        isByPerson = true
    }

    private fun start(move: CameraTransition, inset: MapInset) {
        val number = ++moves
        goal = move.camera
        goalInset = inset
        if (state == CameraMoveState.Idle) port.lift()
        state = CameraMoveState.Moving(number = number, motion = move.motion, durationMillis = move.durationMillis)
        Timber.d(startLine(move, inset))
        port.move(camera = move.camera, inset = inset, motion = move.motion, millis = move.durationMillis) {
            port.post { settle(number) }
        }
    }

    private fun settle(number: Int) {
        val at = port.camera
        val isReached = goal?.isSameView(at) != false
        Timber.d(endLine(isDone = isReached, isCurrent = number == moves, at = at))
        if (number != moves) return
        val target = goal
        val isCut = !isReached && !isByPerson && !isResumed
        if (isCut && target != null) {
            isResumed = true
            start(move = CameraTransition(target, CameraMotion.EASE, RESUME_MILLIS), inset = goalInset)
            return
        }
        state = CameraMoveState.Idle
        port.rest(limits)
        if (!goalInset.isSameInset(port.inset)) padNow(goalInset)
    }

    private fun padNow(next: MapInset) {
        Timber.d(paddingLine(next))
        start(move = CameraTransition(port.camera, CameraMotion.EASE, PADDING_MILLIS), inset = next)
    }
}

/** Czy [other] to to samo dopełnienie: krawędzie różnią się o mniej niż piksel. Wymagania: NFR-56 */
internal fun MapInset.isSameInset(other: MapInset): Boolean =
    abs(top - other.top) < SAME_INSET_PX && abs(bottom - other.bottom) < SAME_INSET_PX
