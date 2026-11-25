/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.dostepnykampus.core.data.export.BarrierExport
import io.github.dostepnykampus.core.data.export.MediaStoreDownloads
import io.github.dostepnykampus.core.data.export.ServerExport
import io.github.dostepnykampus.core.network.export.ExportApi
import java.time.LocalDate
import kotlin.coroutines.CoroutineContext

/** Eksport Koordynatora w grafie Hilt aplikacji. */
@Module
@InstallIn(SingletonComponent::class)
internal object ExportDataModule {
    /** Eksport z serwera zapisany w „Pobrane” telefonu. Wymagania: US13 */
    @Provides
    fun barrierExport(
        api: ExportApi,
        @ApplicationContext context: Context,
        @IoContext work: CoroutineContext,
    ): BarrierExport =
        ServerExport(
            fetch = api::download,
            folder = MediaStoreDownloads(context.contentResolver),
            today = LocalDate::now,
            work = work,
        )
}
