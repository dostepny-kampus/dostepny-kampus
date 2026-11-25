/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.location

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Odczyty położenia wersji debug: trasa pokazu przy włączonym [DemoMode], inaczej dostawca GPS. Wymagania: US6.1 */
@OptIn(TrybPokazu::class)
internal fun variantPositions(context: Context): PositionSource =
    if (DemoMode.isEnabled(context)) {
        DemoRoute.load(context).positions(LocationModule.clock())
    } else {
        GnssPositionFixes(context)
    }

/** Stan satelitów wersji debug: trasa pokazu przy włączonym [DemoMode], inaczej `GnssStatus`. Wymagania: US6.5 */
@OptIn(TrybPokazu::class)
internal fun variantSatellites(context: Context): SatelliteSource =
    if (DemoMode.isEnabled(context)) {
        DemoRoute.load(context).satellites(LocationModule.clock())
    } else {
        GnssSatellites(context)
    }

/** Włącznik lokalizacji wersji debug: zawsze włączony przy [DemoMode], inaczej dostawca GPS. */
@OptIn(TrybPokazu::class)
internal fun variantLocationSwitch(context: Context): LocationSwitch =
    if (DemoMode.isEnabled(context)) {
        LocationSwitch { alwaysEnabled() }
    } else {
        GnssLocationSwitch(context)
    }

private fun alwaysEnabled(): Flow<Boolean> = flowOf(true)
