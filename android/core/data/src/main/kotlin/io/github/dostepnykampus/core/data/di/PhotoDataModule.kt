/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.di

import android.app.Application
import android.os.StatFs
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.dostepnykampus.core.data.photo.PhotoSpace
import io.github.dostepnykampus.core.data.photo.StoragePhotoSpace

/** Miejsce na zdjęcia `core:data` w grafie Hilt aplikacji. */
@Module
@InstallIn(SingletonComponent::class)
internal object PhotoDataModule {
    /** Wolne miejsce w pamięci danych aplikacji, w której leżą zdjęcia zgłoszeń. Wymagania: NFR-7 */
    @Provides
    fun photoSpace(app: Application): PhotoSpace = StoragePhotoSpace { StatFs(app.filesDir.path).availableBytes }
}
