/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.dostepnykampus.core.data.inspection.FloorMarkers
import io.github.dostepnykampus.core.data.inspection.floorMarkersOf
import io.github.dostepnykampus.core.database.inspection.PendingMarkerChangeDao

/** Wiązania Hilt znaczników piętra obejrzanego. */
@Module
@InstallIn(SingletonComponent::class)
internal object InspectionDataModule {
    /** Znaczniki piętra obejrzanego z kolejką w bazie telefonu. Wymagania: US15.3, BR-9 */
    @Provides
    fun floorMarkers(queue: PendingMarkerChangeDao): FloorMarkers = floorMarkersOf(queue)
}
