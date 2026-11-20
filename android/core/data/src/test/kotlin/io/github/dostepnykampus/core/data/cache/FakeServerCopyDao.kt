/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.cache

import io.github.dostepnykampus.core.database.cache.ServerCopyDao
import io.github.dostepnykampus.core.database.cache.ServerCopyEntity

/** Kopie odpowiedzi serwera w pamięci testu: wiersz na klucz, więc test JVM obejdzie się bez Room i Robolectric. */
internal class FakeServerCopyDao : ServerCopyDao {
    val rows: MutableMap<String, ServerCopyEntity> = mutableMapOf()

    override suspend fun get(key: String): ServerCopyEntity? = rows[key]

    override suspend fun save(entity: ServerCopyEntity) {
        rows[entity.key] = entity
    }
}
