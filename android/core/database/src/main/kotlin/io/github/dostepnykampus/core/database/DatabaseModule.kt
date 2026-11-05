/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
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
            .addMigrations(MIGRATION_1_2)
            .build()

    /** Kolejka zgłoszeń z [database]. */
    @Provides
    fun pendingSubmissions(database: DkDatabase): PendingSubmissionDao = database.pendingSubmissions()

    /** Kopia listy budynków z [database] (NFR-10). */
    @Provides
    fun buildingCache(database: DkDatabase): BuildingCacheDao = database.buildingCache()
}
