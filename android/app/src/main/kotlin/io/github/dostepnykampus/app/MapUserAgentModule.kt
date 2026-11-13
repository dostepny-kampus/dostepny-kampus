/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.dostepnykampus.feature.map.MapUserAgent

/** Nagłówek User-Agent kafelków mapy z wersji i identyfikatora aplikacji, w grafie Hilt. Wymagania: NFR-39 */
@Module
@InstallIn(SingletonComponent::class)
object MapUserAgentModule {
    /** Np. „DostepnyKampus/0.1.0 (io.github.dostepnykampus)”. */
    @Provides
    @MapUserAgent
    fun mapUserAgent(): String = "DostepnyKampus/${BuildConfig.VERSION_NAME} (${BuildConfig.APPLICATION_ID})"
}
