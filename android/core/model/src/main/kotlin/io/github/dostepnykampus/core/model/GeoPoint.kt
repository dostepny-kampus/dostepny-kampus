/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.model

/**
 * Punkt geograficzny w WGS84 (EPSG:4326), odpowiednik `GeoPoint` z kontraktu API.
 *
 * @property longitude długość geograficzna w stopniach
 * @property latitude szerokość geograficzna w stopniach
 */
data class GeoPoint(val longitude: Double, val latitude: Double)
