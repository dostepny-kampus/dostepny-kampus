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
import io.github.dostepnykampus.core.network.submission.SubmissionsApi

/** Sprawdzanie zgłoszeń `core:data` w grafie Hilt aplikacji. */
@Module
@InstallIn(SingletonComponent::class)
public object ReviewDataModule {
    /** Kolejka, wzięcie, decyzja i podobne zgłoszenia Weryfikatora z serwera. Wymagania: US10, BR-11 */
    @Provides
    public fun reviews(api: ReviewApi, submissions: SubmissionsApi): Reviews =
        reviewsOf(api = api, submissions = submissions)
}
