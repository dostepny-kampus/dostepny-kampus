/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
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
import io.github.dostepnykampus.core.data.buildings.ServerEntrances
import io.github.dostepnykampus.core.data.buildings.ServerEntrancesRepository
import io.github.dostepnykampus.core.data.buildings.assetRepository
import io.github.dostepnykampus.core.data.map.MapListChoice
import io.github.dostepnykampus.core.data.map.MapListChoiceRepository
import io.github.dostepnykampus.core.data.map.mapDataStore
import io.github.dostepnykampus.core.data.network.ConnectivityNetworkStatus
import io.github.dostepnykampus.core.data.network.NetworkStatus
import io.github.dostepnykampus.core.data.start.FirstLaunch
import io.github.dostepnykampus.core.data.start.FirstLaunchRepository
import io.github.dostepnykampus.core.data.start.startDataStore
import io.github.dostepnykampus.core.data.submissions.SubmissionQueue
import io.github.dostepnykampus.core.data.submissions.SubmissionServer
import io.github.dostepnykampus.core.data.submissions.SubmissionSyncScheduler
import io.github.dostepnykampus.core.database.BuildingCacheDao
import io.github.dostepnykampus.core.database.OwnSubmissionDao
import io.github.dostepnykampus.core.database.PendingSubmissionDao
import io.github.dostepnykampus.core.network.BuildingsApi
import io.github.dostepnykampus.core.network.SubmissionsApi
import javax.inject.Singleton

/**
 * Źródła danych `core:data` w grafie Hilt aplikacji (decyzja 97). Każde ma jedną instancję na proces, więc po obrocie
 * ekranu albo na innym ekranie katalog budynków jest ten sam i czytany raz.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    /** Katalog: kontury z serwera, a przy innej odpowiedzi z kopii w bazie albo z pliku (US1, NFR-10). */
    @Provides
    @Singleton
    fun buildingCatalog(
        app: Application,
        api: BuildingsApi,
        buildingCache: BuildingCacheDao,
    ): BuildingCatalogRepository =
        assetRepository(
            assets = app.assets,
            listBuildings = api::listBuildings,
            buildingCache = buildingCache,
        )

    /** Wejścia karty budynku z serwera z dniem i źródłem oznaczenia (US17.3). */
    @Provides
    @Singleton
    fun cardEntrances(api: BuildingsApi): ServerEntrances = ServerEntrancesRepository(api::building)

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

    /** Planowanie wysyłki kolejki po powrocie sieci (NFR-6); start aplikacji zwołuje je osobno. */
    @Provides
    @Singleton
    fun submissionSync(app: Application): SubmissionSyncScheduler = SubmissionSyncScheduler(app)

    /** Kolejka zgłoszeń: zapis w bazie Room, wysyłka klientem zgłoszeń (NFR-5, US9.2). */
    @Provides
    @Singleton
    fun submissionQueue(
        api: SubmissionsApi,
        store: PendingSubmissionDao,
        sync: SubmissionSyncScheduler,
        history: OwnSubmissionDao,
    ): SubmissionQueue =
        SubmissionQueue(
            store = store,
            server = SubmissionServer(create = api::create, putPhoto = api::putPhoto, update = api::update),
            onSaved = sync::schedule,
            history = history,
        )
}
