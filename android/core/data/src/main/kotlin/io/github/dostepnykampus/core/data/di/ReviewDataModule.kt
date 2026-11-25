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
import io.github.dostepnykampus.core.data.review.Reviews
import io.github.dostepnykampus.core.data.review.reviewsOf
import io.github.dostepnykampus.core.network.review.ReviewApi

/** Sprawdzanie zgłoszeń `core:data` w grafie Hilt aplikacji. */
@Module
@InstallIn(SingletonComponent::class)
internal object ReviewDataModule {
    /** Kolejka, wzięcie, decyzja i podobne zgłoszenia Weryfikatora z serwera. Wymagania: US10, BR-11 */
    @Provides
    fun reviews(api: ReviewApi): Reviews = reviewsOf(api)
}
