/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.dostepnykampus.core.database.cache.ServerCopyDao
import io.github.dostepnykampus.core.database.cache.ServerCopyEntity
import io.github.dostepnykampus.core.database.inspection.InspectionMarkerDao
import io.github.dostepnykampus.core.database.inspection.InspectionMarkerEntity
import io.github.dostepnykampus.core.database.inspection.PendingMarkerChangeDao
import io.github.dostepnykampus.core.database.inspection.PendingMarkerChangeEntity
import io.github.dostepnykampus.core.database.room.FloorPlanDao
import io.github.dostepnykampus.core.database.room.FloorPlanEntity
import io.github.dostepnykampus.core.database.room.FloorRoomEntity
import io.github.dostepnykampus.core.database.submission.OwnSubmissionDao
import io.github.dostepnykampus.core.database.submission.OwnSubmissionEntity
import io.github.dostepnykampus.core.database.submission.PendingEditDao
import io.github.dostepnykampus.core.database.submission.PendingPhotoEntity
import io.github.dostepnykampus.core.database.submission.PendingSubmissionDao
import io.github.dostepnykampus.core.database.submission.PendingSubmissionEntity

/**
 * Baza lokalna aplikacji (Room): zgłoszenia czekające na wysłanie,
 * historia własnych zgłoszeń oraz kopie listy budynków, barier ze zgłoszeń przyjętych i planów pięter.
 *
 * Wymagania: NFR-5, US8, NFR-10, US16.1
 */
@Database(
    entities = [
        PendingSubmissionEntity::class,
        PendingPhotoEntity::class,
        ServerCopyEntity::class,
        OwnSubmissionEntity::class,
        PendingMarkerChangeEntity::class,
        FloorPlanEntity::class,
        FloorRoomEntity::class,
        InspectionMarkerEntity::class,
    ],
    version = 12,
    autoMigrations = [AutoMigration(from = 9, to = 10), AutoMigration(from = 10, to = 11)],
    exportSchema = true,
)
internal abstract class DkDatabase : RoomDatabase() {
    /** Kolejka zgłoszeń. */
    abstract fun pendingSubmissions(): PendingSubmissionDao

    /** Kopie odpowiedzi serwera: lista budynków, bariery ze zgłoszeń przyjętych. */
    abstract fun serverCopy(): ServerCopyDao

    /** Historia własnych zgłoszeń. */
    abstract fun ownSubmissions(): OwnSubmissionDao

    /** Odczyt zgłoszenia z kolejki do poprawki. */
    abstract fun pendingEdits(): PendingEditDao

    /** Przypisanie właściciela wierszom obu kolejek po migracji. */
    abstract fun queueOwnership(): QueueOwnershipDao

    /** Kolejka zmian znaczników piętra obejrzanego. */
    abstract fun pendingMarkerChanges(): PendingMarkerChangeDao

    /** Kopie planów pięter i pomieszczeń. */
    abstract fun floorPlans(): FloorPlanDao

    /** Kopia znaczników piętra obejrzanego z serwera. */
    abstract fun inspectionMarkers(): InspectionMarkerDao
}
