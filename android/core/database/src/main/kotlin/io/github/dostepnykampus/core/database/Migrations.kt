/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.dostepnykampus.core.database.cache.BARRIERS_COPY
import io.github.dostepnykampus.core.database.cache.BUILDINGS_COPY

private const val LIST_COPY_COLUMNS =
    "(`id` INTEGER NOT NULL, `list_json` TEXT NOT NULL, `synced_at` INTEGER NOT NULL, PRIMARY KEY(`id`))"

/** Migracja 1→2: tabela kopii listy budynków z serwera. Wymagania: NFR-10 */
internal val MIGRATION_1_2: Migration =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `building_cache` $LIST_COPY_COLUMNS")
        }
    }

/** Migracja 2→3: historia własnych zgłoszeń dla listy C3. Wymagania: US8, NFR-5 */
internal val MIGRATION_2_3: Migration =
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

/** Migracja 3→4: znacznik odrzuconego zgłoszenia w kolejce, które nie wraca do wysyłki samo. Wymagania: BR-26 */
internal val MIGRATION_3_4: Migration =
    object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `pending_submissions` ADD COLUMN `refused` INTEGER NOT NULL DEFAULT 0")
        }
    }

/** Migracja 4→5: status z serwera i powód odrzucenia w historii własnych zgłoszeń. Wymagania: US8.2, BR-16 */
internal val MIGRATION_4_5: Migration =
    object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `own_submissions` ADD COLUMN `server_status` TEXT")
            db.execSQL("ALTER TABLE `own_submissions` ADD COLUMN `rejection_reason` TEXT")
        }
    }

/** Migracja 5→6: tabela kopii barier ze zgłoszeń przyjętych. Wymagania: NFR-10 */
internal val MIGRATION_5_6: Migration =
    object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `barrier_cache` $LIST_COPY_COLUMNS")
        }
    }

/** Migracja 6→7: kolejka zmian znaczników piętra obejrzanego. Wymagania: US15.3 */
internal val MIGRATION_6_7: Migration =
    object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `pending_marker_changes` (" +
                    "`operation_id` TEXT NOT NULL, `type` TEXT NOT NULL, `marker_id` TEXT NOT NULL, " +
                    "`building_symbol` TEXT NOT NULL, `floor_id` TEXT, `saved_at` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`operation_id`))",
            )
        }
    }

/** Migracja 7→8: kopie planów pięter i pomieszczeń. Wymagania: US16.1, NFR-10 */
internal val MIGRATION_7_8: Migration =
    object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `floor_plans` (" +
                    "`building_symbol` TEXT NOT NULL, `floor_id` TEXT NOT NULL, `label` TEXT NOT NULL, " +
                    "`sort_order` INTEGER NOT NULL, `source` TEXT NOT NULL, " +
                    "`outline_json` TEXT NOT NULL, `synced_at` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`building_symbol`, `floor_id`))",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `floor_rooms` (" +
                    "`building_symbol` TEXT NOT NULL, `floor_id` TEXT NOT NULL, `id` TEXT NOT NULL, " +
                    "`number` TEXT, `function` TEXT NOT NULL, `category` TEXT NOT NULL, " +
                    "`geometry_json` TEXT NOT NULL, `label_longitude` REAL NOT NULL, " +
                    "`label_latitude` REAL NOT NULL, `labelled` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`building_symbol`, `floor_id`, `id`))",
            )
        }
    }

/** Migracja 8→9: piętra windy przy pomieszczeniach kopii planu. Wymagania: US16.1, NFR-10 */
internal val MIGRATION_8_9: Migration =
    object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `floor_rooms` ADD COLUMN `lift_id` TEXT")
            db.execSQL("ALTER TABLE `floor_rooms` ADD COLUMN `lift_floors_json` TEXT")
        }
    }

/**
 * Migracja 11→12: kopie listy budynków i barier przechodzą do wspólnej tabeli `server_copy` pod kluczami
 * [BUILDINGS_COPY] i [BARRIERS_COPY]; stare tabele znikają. Wymagania: NFR-10
 */
internal val MIGRATION_11_12: Migration =
    object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `server_copy` (`key` TEXT NOT NULL, `document_json` TEXT NOT NULL, " +
                    "`synced_at` INTEGER NOT NULL, PRIMARY KEY(`key`))",
            )
            db.execSQL(
                "INSERT INTO `server_copy` SELECT '$BUILDINGS_COPY', `list_json`, `synced_at` FROM `building_cache`",
            )
            db.execSQL(
                "INSERT INTO `server_copy` SELECT '$BARRIERS_COPY', `list_json`, `synced_at` FROM `barrier_cache`",
            )
            db.execSQL("DROP TABLE `building_cache`")
            db.execSQL("DROP TABLE `barrier_cache`")
        }
    }

/** Migracja 12→13: historia „Moje zgłoszenia” dostaje kolumnę właściciela konta. Wymagania: NFR-33, US8.1 */
internal val MIGRATION_12_13: Migration =
    object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `own_submissions` ADD COLUMN `owner_sub` TEXT")
        }
    }

/**
 * Migracja 13→14: historia „Moje zgłoszenia” dostaje oznaczenie „bariera usunięta”, wejście i nowe oznaczenie
 * zmiany oznaczenia wejścia. Wymagania: US8.4, US17.2
 */
internal val MIGRATION_13_14: Migration =
    object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `own_submissions` ADD COLUMN `barrier_removed` INTEGER")
            db.execSQL("ALTER TABLE `own_submissions` ADD COLUMN `entrance_id` TEXT")
            db.execSQL("ALTER TABLE `own_submissions` ADD COLUMN `entrance_marking` TEXT")
        }
    }

/**
 * Migracja 14→15: historia „Moje zgłoszenia” dostaje pełną treść z ostatniego zapisu do poprawki bez sieci.
 * Wymagania: US8.3, NFR-5
 */
internal val MIGRATION_14_15: Migration =
    object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `own_submissions` ADD COLUMN `content_json` TEXT")
        }
    }

/** Wszystkie migracje od wersji 1 w kolejności wersji. */
internal val ALL_MIGRATIONS: List<Migration> =
    listOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6,
        MIGRATION_6_7,
        MIGRATION_7_8,
        MIGRATION_8_9,
        MIGRATION_11_12,
        MIGRATION_12_13,
        MIGRATION_13_14,
        MIGRATION_14_15,
    )
