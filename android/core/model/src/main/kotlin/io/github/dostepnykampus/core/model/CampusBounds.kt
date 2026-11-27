/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.model

/**
 * Prostokąt na mapie w stopniach.
 *
 * Wymagania: NFR-39
 *
 * @property west długość geograficzna zachodniego brzegu
 * @property south szerokość geograficzna południowego brzegu
 * @property east długość geograficzna wschodniego brzegu
 * @property north szerokość geograficzna północnego brzegu
 */
public data class CampusBounds(val west: Double, val south: Double, val east: Double, val north: Double)
