/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import kotlin.coroutines.CoroutineContext

/** Kontekst pracy na plikach, bazie i sieci, poza wątkiem interfejsu. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoContext

/** Kontekst obliczeń, np. geometrii konturów, poza wątkiem interfejsu. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ComputeContext

/**
 * Konteksty pracy poza wątkiem interfejsu w grafie Hilt. Testy Hilt podmieniają ten moduł, żeby `waitForIdle`
 * obejmował pracę bez czekania oknem zegara. Konteksty są parametrami domyślnymi, jak w klasach danych.
 */
@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {
    /** [Dispatchers.IO] w aplikacji. */
    @Provides
    @IoContext
    fun ioWork(): CoroutineContext = ioDefault()

    /** [Dispatchers.Default] w aplikacji. */
    @Provides
    @ComputeContext
    fun computeWork(): CoroutineContext = computeDefault()

    private fun ioDefault(context: CoroutineContext = Dispatchers.IO): CoroutineContext = context

    private fun computeDefault(context: CoroutineContext = Dispatchers.Default): CoroutineContext = context
}
