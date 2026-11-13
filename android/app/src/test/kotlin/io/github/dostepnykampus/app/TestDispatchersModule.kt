/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.github.dostepnykampus.core.data.di.ComputeContext
import io.github.dostepnykampus.core.data.di.DispatchersModule
import io.github.dostepnykampus.core.data.di.IoContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Testy Hilt liczą w wątku wywołującym, więc `waitForIdle` obejmuje całą pracę grafu bez czekania oknem zegara.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DispatchersModule::class])
object TestDispatchersModule {
    /** Praca na plikach i sieci w wątku wywołującym. */
    @Provides
    @IoContext
    fun ioWork(): CoroutineContext = EmptyCoroutineContext

    /** Obliczenia w wątku wywołującym. */
    @Provides
    @ComputeContext
    fun computeWork(): CoroutineContext = EmptyCoroutineContext
}
