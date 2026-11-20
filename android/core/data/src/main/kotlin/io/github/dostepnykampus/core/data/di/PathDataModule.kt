/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.dostepnykampus.core.data.path.AccessiblePaths
import io.github.dostepnykampus.core.data.path.ServerAccessiblePaths
import io.github.dostepnykampus.core.data.path.pathCopy
import io.github.dostepnykampus.core.data.server.toReply
import io.github.dostepnykampus.core.database.cache.ServerCopyDao
import io.github.dostepnykampus.core.network.building.BuildingsApi
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/** Źródło ścieżek dostosowanych `core:data` w grafie Hilt aplikacji. */
@Module
@InstallIn(SingletonComponent::class)
internal object PathDataModule {
    /** Ścieżki dostosowane dla mapy, z kopią w bazie na czas bez sieci. Wymagania: US1.4, NFR-10 */
    @Provides
    @Singleton
    fun accessiblePaths(
        api: BuildingsApi,
        copies: ServerCopyDao,
        @IoContext ioContext: CoroutineContext,
    ): AccessiblePaths =
        ServerAccessiblePaths(
            copy = pathCopy(copies, ioContext),
            server = { api.accessiblePaths().toReply { it.paths } },
        )
}
