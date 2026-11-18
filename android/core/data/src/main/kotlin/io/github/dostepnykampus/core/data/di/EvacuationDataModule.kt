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
import io.github.dostepnykampus.core.data.building.ServerEvacuationExits
import io.github.dostepnykampus.core.data.building.ServerEvacuationExitsRepository
import io.github.dostepnykampus.core.data.server.ServerCard
import io.github.dostepnykampus.core.data.server.toReply
import io.github.dostepnykampus.core.network.building.BuildingsApi
import javax.inject.Singleton

/** Wyjścia ewakuacyjne `core:data` w grafie Hilt aplikacji; osobno od [DataModule], żeby ten nie rósł. */
@Module
@InstallIn(SingletonComponent::class)
internal object EvacuationDataModule {
    /**
     * Wyjścia ewakuacyjne karty budynku z serwera.
     * TODO(#3165): karta V2 jeszcze nie pyta tego źródła.
     *
     * Wymagania: US2.6, BR-27, NFR-10
     */
    @Provides
    @Singleton
    fun evacuationExits(api: BuildingsApi): ServerEvacuationExits =
        ServerEvacuationExitsRepository { symbol ->
            api.building(symbol).toReply { ServerCard(it.building, it.entrances, it.evacuationExits) }
        }
}
