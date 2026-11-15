/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.location

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.dostepnykampus.core.geo.CampusGrounds
import io.github.dostepnykampus.core.geo.PwrGrounds

/** Teren, na którym wolno złożyć zgłoszenie: granice terenów PWr z `core:geo`. Wymagania: BR-G5, US4.9 */
@Module
@InstallIn(SingletonComponent::class)
object CampusGroundsModule {
    /** Teren PWr z pliku granic terenów wbudowanego w aplikację. */
    @Provides
    fun campusGrounds(): CampusGrounds = PwrGrounds
}
