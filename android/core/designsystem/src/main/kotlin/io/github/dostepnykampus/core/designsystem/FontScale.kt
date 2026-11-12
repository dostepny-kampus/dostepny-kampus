/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

/**
 * Skala czcionki telefonu, od której ekrany przechodzą na układ dla dużej czcionki: tytuł pod strzałką,
 * lista zamiast mapy, krótka atrybucja i tryb karty `Direct` (NFR-19, NFR-20). Jedna wartość dla wszystkich
 * ekranów, żeby nagłówek i treść pod nim przełączały się przy tej samej skali.
 */
const val LARGE_FONT_SCALE = 1.5f
