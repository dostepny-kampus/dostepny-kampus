/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Baza lokalna aplikacji (Room): zgłoszenia czekające na wysłanie (NFR-5).
 */
@Database(entities = [PendingSubmissionEntity::class, PendingPhotoEntity::class], version = 1, exportSchema = true)
abstract class DkDatabase : RoomDatabase() {
    /** Kolejka zgłoszeń. */
    abstract fun pendingSubmissions(): PendingSubmissionDao
}
