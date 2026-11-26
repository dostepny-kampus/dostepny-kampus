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
import io.github.dostepnykampus.core.data.room.FloorPlanCopy
import io.github.dostepnykampus.core.data.room.FloorPlans
import io.github.dostepnykampus.core.data.room.RoomSearch
import io.github.dostepnykampus.core.data.room.roomSearchOf
import io.github.dostepnykampus.core.data.server.toReply
import io.github.dostepnykampus.core.database.room.FloorPlanDao
import io.github.dostepnykampus.core.network.building.BuildingsApi
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/** Plany pięter i wyszukiwanie sali `core:data` w grafie Hilt aplikacji; osobno od [DataModule], żeby ten nie rósł. */
@Module
@InstallIn(SingletonComponent::class)
internal object RoomDataModule {
    /** Plany pięter z serwera z kopią w bazie na czas bez sieci. Wymagania: US16.1, NFR-10 */
    @Provides
    @Singleton
    fun floorPlans(
        api: BuildingsApi,
        dao: FloorPlanDao,
        @IoContext ioContext: CoroutineContext,
    ): FloorPlans =
        FloorPlanCopy(
            dao = dao,
            fetchFloors = { symbol -> api.floors(symbol).toReply { it } },
            fetchRooms = { symbol, floor -> api.rooms(symbol, floor).toReply { it } },
            ioContext = ioContext,
        )

    /** Wyszukiwanie sali na serwerze. Wymagania: US18.1 */
    @Provides
    @Singleton
    fun roomSearch(api: BuildingsApi): RoomSearch = roomSearchOf(api)
}
