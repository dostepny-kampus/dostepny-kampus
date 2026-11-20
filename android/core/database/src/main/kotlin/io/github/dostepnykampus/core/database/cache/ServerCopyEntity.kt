/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.database.cache

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Kopia jednej odpowiedzi serwera w telefonie: dokument JSON pod kluczem [key] z chwilą pobrania.
 *
 * Wymagania: NFR-10
 *
 * @property key rodzaj kopii, np. [BUILDINGS_COPY]; jeden wiersz na rodzaj
 * @property documentJson dokument kopii zapisany przez `core:data`
 * @property syncedAt chwila pobrania z serwera w milisekundach epoki
 */
@Entity(tableName = "server_copy")
public data class ServerCopyEntity(
    @PrimaryKey val key: String,
    @ColumnInfo(name = "document_json") val documentJson: String,
    @ColumnInfo(name = "synced_at") val syncedAt: Long,
)

/** Klucz kopii listy budynków. */
public const val BUILDINGS_COPY: String = "buildings"

/** Klucz kopii barier ze zgłoszeń przyjętych. */
public const val BARRIERS_COPY: String = "barriers"
