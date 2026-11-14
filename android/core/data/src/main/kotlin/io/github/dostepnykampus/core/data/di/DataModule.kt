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
import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.buildings.BuildingFloors
import io.github.dostepnykampus.core.data.buildings.BuildingListStore
import io.github.dostepnykampus.core.data.buildings.ServerEntrances
import io.github.dostepnykampus.core.data.buildings.ServerEntrancesRepository
import io.github.dostepnykampus.core.data.buildings.ServerFloorsRepository
import io.github.dostepnykampus.core.data.buildings.assetRepository
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
import io.github.dostepnykampus.core.data.submissions.OwnStatuses
import io.github.dostepnykampus.core.data.submissions.OwnSubmissions
import io.github.dostepnykampus.core.data.submissions.PendingEdits
import io.github.dostepnykampus.core.data.submissions.SubmissionQueue
import io.github.dostepnykampus.core.data.submissions.SubmissionServer
import io.github.dostepnykampus.core.data.submissions.SubmissionStore
import io.github.dostepnykampus.core.data.submissions.SubmissionSyncScheduler
import io.github.dostepnykampus.core.data.submissions.ownStatusesOf
import io.github.dostepnykampus.core.data.submissions.ownSubmissionsOf
import io.github.dostepnykampus.core.data.submissions.pendingEditsOf
import io.github.dostepnykampus.core.network.BuildingsApi
import io.github.dostepnykampus.core.network.SubmissionsApi
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

    /** Wejścia karty budynku z serwera z dniem i źródłem oznaczenia. Wymagania: US17.3 */
    @Provides
    @Singleton
    public fun cardEntrances(api: BuildingsApi): ServerEntrances =
        ServerEntrancesRepository { symbol ->
            api.building(symbol).toReply { ServerCard(it.building, it.entrances) }
        }

    /** Piętra budynku szczegółowego z serwera do formularza C2. Wymagania: US7.2 */
    @Provides
    @Singleton
    public fun buildingFloors(api: BuildingsApi): BuildingFloors =
        ServerFloorsRepository { symbol -> api.floors(symbol).toReply { it } }

    /** Wybór „Mapa” albo „Lista budynków” w pliku ustawień mapy. Wymagania: NFR-19 */
    @Provides
    @Singleton
    public fun mapListChoice(app: Application): MapListChoice = MapListChoiceRepository(app.mapDataStore)

    /** Stan internetu z usługi systemowej telefonu. Wymagania: US9.1 */
    @Provides
    @Singleton
    public fun networkStatus(app: Application): NetworkStatus =
        ConnectivityNetworkStatus(app.getSystemService(ConnectivityManager::class.java))

    /** Pierwsze uruchomienie w pliku ustawień startu. Wymagania: US5.1 */
    @Provides
    @Singleton
    public fun firstLaunch(app: Application): FirstLaunch = FirstLaunchRepository(app.startDataStore)

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
        @IoContext ioContext: CoroutineContext,
    ): SubmissionQueue =
        SubmissionQueue(
            store = store,
            ioContext = ioContext,
            server =
                SubmissionServer(
                    create = { id, content -> api.create(id, content).toReply { it } },
                    putPhoto = { id, photo, jpeg ->
                        api.putPhoto(submissionId = id, photoId = photo, jpeg = jpeg).toReply {}
                    },
                    update = { id, version, content ->
                        api.update(id = id, version = version, content = content).toReply { it.toServerEdit() }
                    },
                ),
            onSaved = sync::scheduleAfterSave,
        )

    /** Własne zgłoszenia dla listy C3 z historii w telefonie. Wymagania: US8.1, NFR-5 */
    @Provides
    public fun ownSubmissions(store: SubmissionStore): OwnSubmissions = ownSubmissionsOf(store.history)

    /** Zgłoszenia z kolejki telefonu do poprawki w C2. Wymagania: US8.3 */
    @Provides
    public fun pendingEdits(store: SubmissionStore): PendingEdits = pendingEditsOf(store.edits)

    /** Odświeżenie statusów listy C3 z serwera. Wymagania: US8.2 */
    @Provides
    public fun ownStatuses(api: SubmissionsApi, store: SubmissionStore): OwnStatuses = ownStatusesOf(api::mine, store.history)
}
