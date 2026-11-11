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

/** Migracja 3→4: znacznik odrzuconego zgłoszenia w kolejce, które nie wraca do wysyłki samo (BR-26). */
val MIGRATION_3_4: Migration =
    object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `pending_submissions` ADD COLUMN `refused` INTEGER NOT NULL DEFAULT 0")
        }
    }

/** Migracja 4→5: status z serwera i powód odrzucenia w historii własnych zgłoszeń (US8.2, BR-16). */
val MIGRATION_4_5: Migration =
    object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `own_submissions` ADD COLUMN `server_status` TEXT")
            db.execSQL("ALTER TABLE `own_submissions` ADD COLUMN `rejection_reason` TEXT")
        }
    }
