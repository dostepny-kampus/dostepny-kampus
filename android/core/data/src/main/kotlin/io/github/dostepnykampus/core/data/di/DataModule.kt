/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.di

import android.app.Application
import android.net.ConnectivityManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.dostepnykampus.core.data.account.GuestRoleProvider
import io.github.dostepnykampus.core.data.account.RoleProvider
import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.buildings.assetRepository
import io.github.dostepnykampus.core.data.map.MapListChoice
import io.github.dostepnykampus.core.data.map.MapListChoiceRepository
import io.github.dostepnykampus.core.data.map.mapDataStore
import io.github.dostepnykampus.core.data.network.ConnectivityNetworkStatus
import io.github.dostepnykampus.core.data.network.NetworkStatus
import io.github.dostepnykampus.core.data.start.FirstLaunch
import io.github.dostepnykampus.core.data.start.FirstLaunchRepository
import io.github.dostepnykampus.core.data.start.startDataStore
import javax.inject.Singleton

/**
 * Źródła danych `core:data` w grafie Hilt aplikacji (decyzja 97). Każde ma jedną instancję na proces, więc po obrocie
 * ekranu albo na innym ekranie katalog budynków jest ten sam i czytany raz.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    /** Katalog budynków z pliku konturów w zasobach aplikacji (US1). */
    @Provides
    @Singleton
    fun buildingCatalog(app: Application): BuildingCatalogRepository = assetRepository(app.assets)

    /** Wybór „Mapa” albo „Lista budynków” w pliku ustawień mapy (NFR-19). */
    @Provides
    @Singleton
    fun mapListChoice(app: Application): MapListChoice = MapListChoiceRepository(app.mapDataStore)

    /** Stan internetu z usługi systemowej telefonu (US9.1). */
    @Provides
    @Singleton
    fun networkStatus(app: Application): NetworkStatus =
        ConnectivityNetworkStatus(app.getSystemService(ConnectivityManager::class.java))

    /** Pierwsze uruchomienie w pliku ustawień startu (US5.1). */
    @Provides
    @Singleton
    fun firstLaunch(app: Application): FirstLaunch = FirstLaunchRepository(app.startDataStore)

    /** Bez logowania w aplikacji każda osoba jest Gościem (RO-GOS, US1.2). */
    @Provides
    @Singleton
    fun roles(): RoleProvider = GuestRoleProvider
}
