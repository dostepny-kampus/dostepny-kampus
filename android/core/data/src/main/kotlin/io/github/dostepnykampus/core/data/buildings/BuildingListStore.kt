/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.buildings

import io.github.dostepnykampus.core.database.BuildingCacheDao
import javax.inject.Inject

/**
 * Kopia listy budynków w telefonie dla [BuildingCatalogRepository]. Daje go Hilt, więc tabela bazy nie wychodzi
 * poza `core:data`.
 *
 * Wymagania: NFR-10
 *
 * @property cache ostatnia lista budynków z serwera
 */
@ConsistentCopyVisibility
public data class BuildingListStore
    @Inject
    internal constructor(internal val cache: BuildingCacheDao)
