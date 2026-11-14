/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submissions

import io.github.dostepnykampus.core.model.GeoPoint

/**
 * Pozycja do treści zgłoszenia szybkiego.
 *
 * @property point miejsce bariery
 * @property accuracyMeters dokładność pozycji w metrach
 * @property isInBuilding czy punkt leży w konturze budynku
 */
data class QuickReportLocation(val point: GeoPoint, val accuracyMeters: Double, val isInBuilding: Boolean)
