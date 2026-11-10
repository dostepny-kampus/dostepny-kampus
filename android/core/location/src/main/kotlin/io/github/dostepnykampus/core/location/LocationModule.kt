/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
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

/** Źródła pomiaru ekranu C1 w grafie Hilt: odbiornik GNSS telefonu i jego zegar monotoniczny (US6.1). */
@Module
@InstallIn(SingletonComponent::class)
object LocationModule {
    /** Odczyty z dostawcy GPS telefonu. */
    @Provides
    fun positions(
        @ApplicationContext context: Context,
    ): PositionSource = GnssPositionFixes(context)

    /** Stan satelitów z `GnssStatus` telefonu. */
    @Provides
    fun satellites(
        @ApplicationContext context: Context,
    ): SatelliteSource = GnssSatellites(context)

    /** Włącznik lokalizacji telefonu z systemowych zdarzeń zmiany dostawców. */
    @Provides
    fun locationSwitch(
        @ApplicationContext context: Context,
    ): LocationSwitch = GnssLocationSwitch(context)

    /** Czas od startu telefonu, ten sam co w odczytach GNSS. */
    @Provides
    fun clock(): ElapsedClock = ElapsedClock(SystemClock::elapsedRealtime)
}
