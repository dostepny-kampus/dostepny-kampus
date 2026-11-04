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
 *
 * Schemat nie jest eksportowany, dopóki baza ma wersję 1; pierwsza migracja doda eksport do repozytorium.
 */
@Database(entities = [PendingSubmissionEntity::class, PendingPhotoEntity::class], version = 1, exportSchema = false)
abstract class DkDatabase : RoomDatabase()
