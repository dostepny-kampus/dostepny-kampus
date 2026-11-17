/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.camera

import kotlin.math.roundToLong

private const val PLACE_MILLIS = 300.0

/**
 * Ten sam widok bez ruchu kamery: mapa, której przed chwilą nie było na ekranie (wybór z listy budynków), staje od
 * razu w widoku końcowym, a karta wjeżdża w 300 ms mnożone przez skalę animacji [animationScale].
 *
 * Wymagania: US2.8, NFR-19, NFR-56
 */
internal fun CameraTransition.placed(animationScale: Float): CameraTransition =
    copy(motion = CameraMotion.NONE, durationMillis = (PLACE_MILLIS * animationScale).roundToLong())
