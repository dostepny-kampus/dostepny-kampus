/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.model

/** Powierzchnia GeoJSON z kontraktu API (`GeoArea`): wielokąt albo wielokąt złożony, np. kontur budynku. */
sealed interface GeoArea

/**
 * Wielokąt GeoJSON (`GeoPolygon` z kontraktu API).
 *
 * @property rings zamknięte pierścienie: pierwszy to granica zewnętrzna, kolejne to otwory, np. dziedziniec
 */
data class GeoPolygon(val rings: List<List<GeoPoint>>) : GeoArea

/**
 * Wielokąt złożony GeoJSON (`GeoMultiPolygon` z kontraktu API), np. budynek z kilku części.
 *
 * @property polygons wielokąty składowe
 */
data class GeoMultiPolygon(val polygons: List<GeoPolygon>) : GeoArea
