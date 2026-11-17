/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction

/** Właściciel wpisów zapisanych przed dodaniem `owner_sub`. Wymagania: BR-9, NFR-33 */
@Dao
public interface QueueOwnershipDao {
    /** Przypisuje stare wpisy obu kolejek do pierwszego konta po migracji. */
    @Transaction
    public suspend fun adopt(subject: String) {
        adoptSubmissions(subject)
        adoptMarkers(subject)
    }

    /** Przypisuje zgłoszenia w ramach [adopt]. */
    @Query("UPDATE pending_submissions SET owner_sub = :subject WHERE owner_sub IS NULL")
    public suspend fun adoptSubmissions(subject: String)

    /** Przypisuje zmiany znaczników w ramach [adopt]. */
    @Query("UPDATE pending_marker_changes SET owner_sub = :subject WHERE owner_sub IS NULL")
    public suspend fun adoptMarkers(subject: String)
}
