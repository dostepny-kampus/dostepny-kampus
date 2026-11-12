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
import io.github.dostepnykampus.core.data.barriers.AcceptedBarriers
import io.github.dostepnykampus.core.data.barriers.ServerAcceptedBarriers
import io.github.dostepnykampus.core.network.SubmissionsApi
import javax.inject.Singleton

/** Źródła barier `core:data` w grafie Hilt aplikacji (decyzja 97); osobno od [DataModule], żeby ten nie rósł. */
@Module
@InstallIn(SingletonComponent::class)
object BarrierDataModule {
    /** Bariery ze zgłoszeń przyjętych dla mapy (US1.3, BR-6). */
    @Provides
    @Singleton
    fun acceptedBarriers(api: SubmissionsApi): AcceptedBarriers = ServerAcceptedBarriers { api.accepted() }
}
