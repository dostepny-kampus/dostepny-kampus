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
import javax.inject.Singleton

/** Baza lokalna w grafie Hilt: jedna instancja na proces. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    private const val NAME = "dostepny-kampus.db"

    /** Baza w pliku [NAME] w pamięci aplikacji; przetrwa zamknięcie aplikacji i restart telefonu (NFR-5). */
    @Provides
    @Singleton
    fun database(
        @ApplicationContext context: Context,
    ): DkDatabase =
        Room
            .databaseBuilder(context, DkDatabase::class.java, NAME)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
            .build()

    /** Kolejka zgłoszeń z [database]. */
    @Provides
    fun pendingSubmissions(database: DkDatabase): PendingSubmissionDao = database.pendingSubmissions()

    /** Kopia listy budynków z [database] (NFR-10). */
    @Provides
    fun buildingCache(database: DkDatabase): BuildingCacheDao = database.buildingCache()

    /** Historia własnych zgłoszeń z [database] (US8). */
    @Provides
    fun ownSubmissions(database: DkDatabase): OwnSubmissionDao = database.ownSubmissions()

    /** Odczyt zgłoszenia z kolejki do poprawki z [database] (US8.3). */
    @Provides
    fun pendingEdits(database: DkDatabase): PendingEditDao = database.pendingEdits()

    /** Kopia barier ze zgłoszeń przyjętych z [database] (NFR-10). */
    @Provides
    fun barrierCache(database: DkDatabase): BarrierCacheDao = database.barrierCache()
}
