/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.cache

import io.github.dostepnykampus.core.database.cache.ServerCopyDao
import io.github.dostepnykampus.core.database.cache.ServerCopyEntity
import kotlinx.coroutines.withContext
import java.time.Instant
import kotlin.coroutines.CoroutineContext

/**
 * Wartość z kopii razem z chwilą jej pobrania z serwera.
 *
 * Wymagania: NFR-10
 *
 * @param T typ wartości kopii
 * @property value wartość z kopii
 * @property syncedAt chwila pobrania z serwera
 */
internal data class Synced<T>(val value: T, val syncedAt: Instant)

/**
 * Kopia ostatniej odpowiedzi serwera jednego rodzaju w wierszu `server_copy` pod kluczem [key].
 *
 * Nowa odpowiedź serwera potrzebuje tylko klucza i kodeka dokumentu, bez nowej tabeli.
 *
 * Wymagania: NFR-10
 *
 * @param T typ wartości kopii
 * @param dao wiersze kopii w bazie
 * @param key rodzaj kopii, np. `BUILDINGS_COPY`
 * @param encode dokument JSON z wartości
 * @param decode wartość z dokumentu; `null` dla zepsutego dokumentu
 * @param ioContext kontekst zapisu i odczytu bazy
 */
internal class ServerCopy<T>(
    private val dao: ServerCopyDao,
    private val key: String,
    private val encode: (T) -> String,
    private val decode: (String) -> T?,
    private val ioContext: CoroutineContext,
) {
    /** Zapisuje [value] z chwilą pobrania [syncedAt], zastępując poprzednią kopię tego rodzaju. */
    suspend fun save(value: T, syncedAt: Instant) {
        val row = ServerCopyEntity(key = key, documentJson = encode(value), syncedAt = syncedAt.toEpochMilli())
        withContext(ioContext) { dao.save(row) }
    }

    /** Wartość z kopii z chwilą pobrania; `null`, gdy kopii nie ma albo jest zepsuta. */
    suspend fun restore(): Synced<T>? {
        val row = withContext(ioContext) { dao.get(key) }
        val value = row?.run { decode(documentJson) }
        return if (row == null || value == null) null else Synced(value, Instant.ofEpochMilli(row.syncedAt))
    }
}
