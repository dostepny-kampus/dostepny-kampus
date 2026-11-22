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
import io.github.dostepnykampus.core.data.barrier.AcceptedBarriers
import io.github.dostepnykampus.core.data.barrier.PhotoAddressBarriers
import io.github.dostepnykampus.core.data.barrier.ServerAcceptedBarriers
import io.github.dostepnykampus.core.data.barrier.barrierCopy
import io.github.dostepnykampus.core.data.server.toReply
import io.github.dostepnykampus.core.database.cache.ServerCopyDao
import io.github.dostepnykampus.core.network.submission.SubmissionsApi
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/** Źródła barier `core:data` w grafie Hilt aplikacji; osobno od [DataModule], żeby ten nie rósł. */
@Module
@InstallIn(SingletonComponent::class)
public object BarrierDataModule {
    /** Bariery ze zgłoszeń przyjętych dla mapy, z kopią w bazie na czas bez sieci. Wymagania: US1.3, BR-6, NFR-10 */
    @Provides
    @Singleton
    public fun acceptedBarriers(
        api: SubmissionsApi,
        copies: ServerCopyDao,
        @IoContext ioContext: CoroutineContext,
    ): AcceptedBarriers =
        PhotoAddressBarriers(
            source =
                ServerAcceptedBarriers(
                    copy = barrierCopy(copies, ioContext),
                    accepted = { api.accepted().toReply { it } },
                ),
            address = api::photoUrl,
        )
}
