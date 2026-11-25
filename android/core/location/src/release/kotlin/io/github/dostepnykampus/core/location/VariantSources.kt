/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.location

import android.content.Context

/** Odczyty położenia wersji release: dostawca GPS telefonu. Wymagania: US6.1 */
internal fun variantPositions(context: Context): PositionSource = GnssPositionFixes(context)

/** Stan satelitów wersji release: `GnssStatus` telefonu. Wymagania: US6.1 */
internal fun variantSatellites(context: Context): SatelliteSource = GnssSatellites(context)

/** Włącznik lokalizacji wersji release: dostawca GPS telefonu. */
internal fun variantLocationSwitch(context: Context): LocationSwitch = GnssLocationSwitch(context)
