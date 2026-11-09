/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Migracja 1→2: tabela kopii listy budynków z serwera (NFR-10). */
val MIGRATION_1_2: Migration =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `building_cache` (" +
                    "`id` INTEGER NOT NULL, `list_json` TEXT NOT NULL, `synced_at` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`id`))",
            )
        }
    }

/** Migracja 2→3: historia własnych zgłoszeń dla listy C3 (US8, NFR-5). */
val MIGRATION_2_3: Migration =
    object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `own_submissions` (" +
                    "`id` TEXT NOT NULL, `saved_at` INTEGER NOT NULL, `kind` TEXT NOT NULL, " +
                    "`building_symbol` TEXT, `floor_number` INTEGER, `floor_id` TEXT, `barrier_type` TEXT, " +
                    "`severity` INTEGER, `state` TEXT NOT NULL, `server_version` INTEGER, `refusal_code` INTEGER, " +
                    "PRIMARY KEY(`id`))",
            )
        }
    }
