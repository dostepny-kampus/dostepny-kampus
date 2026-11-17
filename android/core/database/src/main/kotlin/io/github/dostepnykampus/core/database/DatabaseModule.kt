/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.dostepnykampus.core.database.cache.BarrierCacheDao
import io.github.dostepnykampus.core.database.cache.BuildingCacheDao
import io.github.dostepnykampus.core.database.inspection.InspectionMarkerDao
import io.github.dostepnykampus.core.database.inspection.PendingMarkerChangeDao
import io.github.dostepnykampus.core.database.room.FloorPlanDao
import io.github.dostepnykampus.core.database.submission.OwnSubmissionDao
import io.github.dostepnykampus.core.database.submission.PendingEditDao
import io.github.dostepnykampus.core.database.submission.PendingSubmissionDao
import javax.inject.Singleton

/** Baza lokalna w grafie Hilt: jedna instancja na proces. */
@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    private const val NAME = "dostepny-kampus.db"

    /** Baza w pliku [NAME] w pamięci aplikacji; przetrwa zamknięcie aplikacji i restart telefonu. Wymagania: NFR-5 */
    @Provides
    @Singleton
    fun database(
        @ApplicationContext context: Context,
    ): DkDatabase {
        val builder = Room.databaseBuilder(context, DkDatabase::class.java, NAME)
        ALL_MIGRATIONS.forEach { builder.addMigrations(it) }
        return builder.build()
    }

    /** Kolejka zgłoszeń z [database]. */
    @Provides
    fun pendingSubmissions(database: DkDatabase): PendingSubmissionDao = database.pendingSubmissions()

    /** Kopia listy budynków z [database]. Wymagania: NFR-10 */
    @Provides
    fun buildingCache(database: DkDatabase): BuildingCacheDao = database.buildingCache()

    /** Historia własnych zgłoszeń z [database]. Wymagania: US8 */
    @Provides
    fun ownSubmissions(database: DkDatabase): OwnSubmissionDao = database.ownSubmissions()

    /** Odczyt zgłoszenia z kolejki do poprawki z [database]. Wymagania: US8.3 */
    @Provides
    fun pendingEdits(database: DkDatabase): PendingEditDao = database.pendingEdits()

    /** Kopia barier ze zgłoszeń przyjętych z [database]. Wymagania: NFR-10 */
    @Provides
    fun barrierCache(database: DkDatabase): BarrierCacheDao = database.barrierCache()

    /** Przypisanie właściciela wierszom kolejek utworzonym przed migracją. */
    @Provides
    fun queueOwnership(database: DkDatabase): QueueOwnershipDao = database.queueOwnership()

    /** Kolejka zmian znaczników piętra obejrzanego z [database]. Wymagania: US15.3 */
    @Provides
    fun pendingMarkerChanges(database: DkDatabase): PendingMarkerChangeDao = database.pendingMarkerChanges()

    /** Kopie planów pięter i pomieszczeń z [database]. Wymagania: US16.1, NFR-10 */
    @Provides
    fun floorPlans(database: DkDatabase): FloorPlanDao = database.floorPlans()

    /** Kopia znaczników piętra obejrzanego z serwera z [database]. Wymagania: US15.1, US15.2 */
    @Provides
    fun inspectionMarkers(database: DkDatabase): InspectionMarkerDao = database.inspectionMarkers()
}
