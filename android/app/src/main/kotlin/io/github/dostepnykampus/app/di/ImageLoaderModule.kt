/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.di

import android.content.Context
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

/** Wczytywanie obrazów z serwera w grafie Hilt: jeden [ImageLoader] na proces. Wymagania: NFR-12, NFR-50 */
@Module
@InstallIn(SingletonComponent::class)
object ImageLoaderModule {
    /** [ImageLoader], który pobiera przez [http], więc zdjęcia niosą token konta i dzielą pulę połączeń. */
    @Provides
    @Singleton
    fun imageLoader(
        @ApplicationContext context: Context,
        http: OkHttpClient,
    ): ImageLoader =
        ImageLoader
            .Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { http })) }
            .build()
}

/** Dostęp do [ImageLoader] z grafu dla klasy, której Hilt nie wstrzykuje przez konstruktor. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ImageLoaderEntryPoint {
    /** [ImageLoader] z [ImageLoaderModule]. */
    fun imageLoader(): ImageLoader
}
