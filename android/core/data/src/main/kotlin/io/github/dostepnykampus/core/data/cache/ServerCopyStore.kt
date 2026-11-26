/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.cache

import io.github.dostepnykampus.core.database.cache.ServerCopyDao
import javax.inject.Inject

/**
 * Kopie odpowiedzi serwera w telefonie dla źródeł `core:data`. Daje go Hilt, więc tabela bazy nie wychodzi
 * poza `core:data`.
 *
 * Wymagania: NFR-10
 *
 * @property dao wiersze kopii według klucza
 */
@ConsistentCopyVisibility
public data class ServerCopyStore
    @Inject
    internal constructor(internal val dao: ServerCopyDao)
