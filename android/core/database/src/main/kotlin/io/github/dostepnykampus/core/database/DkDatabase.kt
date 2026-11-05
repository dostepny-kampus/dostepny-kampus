/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Baza lokalna aplikacji (Room): zgłoszenia czekające na wysłanie (NFR-5) i kopia listy budynków (NFR-10).
 */
@Database(
    entities = [PendingSubmissionEntity::class, PendingPhotoEntity::class, BuildingCacheEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class DkDatabase : RoomDatabase() {
    /** Kolejka zgłoszeń. */
    abstract fun pendingSubmissions(): PendingSubmissionDao

    /** Kopia listy budynków. */
    abstract fun buildingCache(): BuildingCacheDao
}
