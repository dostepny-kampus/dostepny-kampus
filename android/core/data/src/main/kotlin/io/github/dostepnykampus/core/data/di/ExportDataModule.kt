/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.di

import android.content.Context
import android.os.Build
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.dostepnykampus.core.data.export.BarrierExport
import io.github.dostepnykampus.core.data.export.ExportPlaces
import io.github.dostepnykampus.core.data.export.MediaStoreDownloads
import io.github.dostepnykampus.core.data.export.ResolverChosenFile
import io.github.dostepnykampus.core.data.export.ServerExport
import io.github.dostepnykampus.core.network.export.ExportApi
import java.time.LocalDate
import kotlin.coroutines.CoroutineContext

/** Eksport Koordynatora w grafie Hilt aplikacji. */
@Module
@InstallIn(SingletonComponent::class)
internal object ExportDataModule {
    /** Eksport z serwera w „Pobrane”, a do Androida 9 w miejscu z okna systemu. Wymagania: US13, NFR-41 */
    @Provides
    fun barrierExport(
        api: ExportApi,
        @ApplicationContext context: Context,
        @IoContext work: CoroutineContext,
    ): BarrierExport =
        ServerExport(
            fetch = api::download,
            places =
                ExportPlaces(
                    downloads = MediaStoreDownloads(context.contentResolver),
                    chosen = ResolverChosenFile(context.contentResolver),
                    isDirect = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
                ),
            today = LocalDate::now,
            work = work,
        )
}
