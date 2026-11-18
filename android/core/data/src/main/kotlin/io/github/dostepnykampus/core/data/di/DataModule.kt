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
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.building.BuildingFloors
import io.github.dostepnykampus.core.data.building.BuildingListStore
import io.github.dostepnykampus.core.data.building.ServerEntrances
import io.github.dostepnykampus.core.data.building.ServerEntrancesRepository
import io.github.dostepnykampus.core.data.building.ServerFloorsRepository
import io.github.dostepnykampus.core.data.building.assetRepository
import io.github.dostepnykampus.core.data.map.MapListChoice
import io.github.dostepnykampus.core.data.map.MapListChoiceRepository
import io.github.dostepnykampus.core.data.map.mapDataStore
import io.github.dostepnykampus.core.data.network.ConnectivityNetworkStatus
import io.github.dostepnykampus.core.data.network.NetworkStatus
import io.github.dostepnykampus.core.data.server.ServerCard
import io.github.dostepnykampus.core.data.server.toReply
import io.github.dostepnykampus.core.data.server.toServerEdit
import io.github.dostepnykampus.core.data.start.FirstLaunch
import io.github.dostepnykampus.core.data.start.FirstLaunchRepository
import io.github.dostepnykampus.core.data.start.startDataStore
import io.github.dostepnykampus.core.data.submission.own.OwnStatuses
import io.github.dostepnykampus.core.data.submission.own.OwnSubmissions
import io.github.dostepnykampus.core.data.submission.own.ownStatusesOf
import io.github.dostepnykampus.core.data.submission.own.ownSubmissionsOf
import io.github.dostepnykampus.core.data.submission.pending.PendingEdits
import io.github.dostepnykampus.core.data.submission.pending.pendingEditsOf
import io.github.dostepnykampus.core.data.submission.sync.SubmissionQueue
import io.github.dostepnykampus.core.data.submission.sync.SubmissionServer
import io.github.dostepnykampus.core.data.submission.sync.SubmissionStore
import io.github.dostepnykampus.core.data.submission.sync.SubmissionSyncScheduler
import io.github.dostepnykampus.core.network.auth.AccessTokens
import io.github.dostepnykampus.core.network.building.BuildingsApi
import io.github.dostepnykampus.core.network.submission.SubmissionsApi
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * Źródła danych `core:data` w grafie Hilt aplikacji. Każde ma jedną instancję na proces, więc po obrocie
 * ekranu albo na innym ekranie katalog budynków jest ten sam i czytany raz.
 */
@Module
@InstallIn(SingletonComponent::class)
public object DataModule {
    /** Katalog: kontury z serwera, a przy innej odpowiedzi z kopii w bazie albo z pliku. Wymagania: US1, NFR-10 */
    @Provides
    @Singleton
    public fun buildingCatalog(
        app: Application,
        api: BuildingsApi,
        buildingList: BuildingListStore,
        @IoContext ioContext: CoroutineContext,
    ): BuildingCatalogRepository =
        assetRepository(
            assets = app.assets,
            ioContext = ioContext,
            listBuildings = { api.listBuildings().toReply { it } },
            buildingCache = buildingList.cache,
        )

    /** Wejścia z dniem i źródłem oznaczenia i wyjścia ewakuacyjne karty budynku z serwera. Wymagania: US17.3, US2.6 */
    @Provides
    @Singleton
    public fun cardEntrances(api: BuildingsApi): ServerEntrances =
        ServerEntrancesRepository { symbol ->
            api.building(symbol).toReply { ServerCard(it.building, it.entrances, it.evacuationExits) }
        }

    /** Piętra budynku szczegółowego z serwera do formularza C2. Wymagania: US7.2 */
    @Provides
    @Singleton
    public fun buildingFloors(api: BuildingsApi): BuildingFloors =
        ServerFloorsRepository { symbol -> api.floors(symbol).toReply { it } }

    /** Widok V1, mapa albo lista, w pliku ustawień mapy. Wymagania: NFR-19 */
    @Provides
    @Singleton
    public fun mapListChoice(app: Application): MapListChoice = MapListChoiceRepository(mapDataStore(app))

    /** Stan internetu z usługi systemowej telefonu. Wymagania: US9.1 */
    @Provides
    @Singleton
    public fun networkStatus(app: Application): NetworkStatus =
        ConnectivityNetworkStatus(app.getSystemService(ConnectivityManager::class.java))

    /** Pierwsze uruchomienie w pliku ustawień startu. Wymagania: US5.1 */
    @Provides
    @Singleton
    public fun firstLaunch(app: Application): FirstLaunch = FirstLaunchRepository(startDataStore(app))

    /** Planowanie wysyłki kolejki po powrocie sieci; start aplikacji zwołuje je osobno. Wymagania: NFR-6 */
    @Provides
    @Singleton
    public fun submissionSync(app: Application): SubmissionSyncScheduler = SubmissionSyncScheduler(app)

    /** Kolejka zgłoszeń: zapis w bazie Room, wysyłka klientem zgłoszeń. Wymagania: NFR-5, US9.2 */
    @Provides
    @Singleton
    public fun submissionQueue(
        api: SubmissionsApi,
        store: SubmissionStore,
        sync: SubmissionSyncScheduler,
        tokens: AccessTokens,
        @IoContext ioContext: CoroutineContext,
    ): SubmissionQueue =
        SubmissionQueue(
            store = store,
            ioContext = ioContext,
            server =
                SubmissionServer(
                    subject = tokens::subject,
                    create = { owner, id, content -> api.create(id, content, owner).toReply { it } },
                    putPhoto = { owner, id, photo, jpeg ->
                        api.putPhoto(submissionId = id, photoId = photo, jpeg = jpeg, owner = owner).toReply {}
                    },
                    update = { owner, id, version, content ->
                        val result = api.update(id = id, version = version, content = content, owner = owner)
                        result.toReply { it.toServerEdit() }
                    },
                ),
            onSaved = sync::scheduleAfterSave,
        )

    /** Własne zgłoszenia dla listy C3 z historii w telefonie. Wymagania: US8.1, NFR-5 */
    @Provides
    public fun ownSubmissions(store: SubmissionStore): OwnSubmissions = ownSubmissionsOf(store.history, store.pending)

    /** Zgłoszenia z kolejki telefonu do poprawki w C2. Wymagania: US8.3 */
    @Provides
    public fun pendingEdits(store: SubmissionStore, tokens: AccessTokens): PendingEdits =
        pendingEditsOf(store.edits, tokens::subject)

    /** Odświeżenie statusów listy C3 z serwera. Wymagania: US8.2 */
    @Provides
    public fun ownStatuses(api: SubmissionsApi, store: SubmissionStore): OwnStatuses =
        ownStatusesOf(api::mine, store.history)
}
