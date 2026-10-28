/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.model

/**
 * Powierzchnia GeoJSON z kontraktu API (`GeoArea`); tak zapisany jest kontur budynku.
 * `MultiPolygon` z kontraktu to lista jego wielokątów składowych, a `Polygon` — lista z jednym wielokątem.
 *
 * @property polygons wielokąty składowe, np. części budynku
 */
data class GeoArea(val polygons: List<GeoPolygon>)

/**
 * Wielokąt GeoJSON (`GeoPolygon` z kontraktu API).
 *
 * @property rings zamknięte pierścienie: pierwszy to granica zewnętrzna, kolejne to otwory, np. dziedziniec
 */
data class GeoPolygon(val rings: List<List<GeoPoint>>)
