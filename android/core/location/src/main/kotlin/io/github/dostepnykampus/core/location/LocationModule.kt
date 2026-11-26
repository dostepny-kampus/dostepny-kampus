/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.location

import android.content.Context
import android.os.SystemClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

/** Źródła pomiaru ekranu C1 w grafie Hilt według wariantu budowy i zegar monotoniczny telefonu. Wymagania: US6.1 */
@Module
@InstallIn(SingletonComponent::class)
internal object LocationModule {
    /** Odczyty położenia: w release z dostawcy GPS telefonu. */
    @Provides
    fun positions(
        @ApplicationContext context: Context,
    ): PositionSource = variantPositions(context)

    /** Stan satelitów: w release z `GnssStatus` telefonu. */
    @Provides
    fun satellites(
        @ApplicationContext context: Context,
    ): SatelliteSource = variantSatellites(context)

    /** Włącznik lokalizacji: w release z systemowych zdarzeń zmiany dostawców. */
    @Provides
    fun locationSwitch(
        @ApplicationContext context: Context,
    ): LocationSwitch = variantLocationSwitch(context)

    /** Czas od startu telefonu, ten sam co w odczytach GNSS. */
    @Provides
    fun clock(): ElapsedClock = ElapsedClock(SystemClock::elapsedRealtime)
}
