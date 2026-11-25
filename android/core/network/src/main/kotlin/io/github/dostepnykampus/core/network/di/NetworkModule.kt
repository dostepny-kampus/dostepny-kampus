/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.dostepnykampus.core.network.BuildConfig
import io.github.dostepnykampus.core.network.auth.AccessTokenInterceptor
import io.github.dostepnykampus.core.network.auth.AccessTokens
import io.github.dostepnykampus.core.network.building.BuildingsApi
import io.github.dostepnykampus.core.network.inspection.InspectionProgressApi
import io.github.dostepnykampus.core.network.invitation.InvitationsApi
import io.github.dostepnykampus.core.network.inspection.MarkerChangesApi
import io.github.dostepnykampus.core.network.submission.SubmissionsApi
import io.github.dostepnykampus.core.network.sync.SyncPullApi
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Klient HTTP `core:network` w grafie Hilt aplikacji.
 *
 * Jedna instancja klienta, parsera, [BuildingsApi], [SubmissionsApi], [MarkerChangesApi] i [SyncPullApi] na proces.
 */
@Module
@InstallIn(SingletonComponent::class)
public object NetworkModule {
    private const val CALL_TIMEOUT_SECONDS = 3L

    /**
     * Klient HTTP: całe wywołanie, z połączeniem i odczytem, trwa najwyżej 3 s. Mapa czeka na listę budynków
     * z serwera przed pierwszym pokazaniem, a bez serwera bierze plik konturów, więc nie może czekać dłużej
     * niż próg mapy z danymi w telefonie. Wysyłka zdjęć ustawi dłuższy limit na kopii klienta
     * z `newBuilder()`. Żądania do serwera aplikacji niosą token konta z [tokens] ([AccessTokenInterceptor]).
     *
     * Wymagania: NFR-10, NFR-11
     */
    @Provides
    @Singleton
    public fun okHttp(tokens: AccessTokens): OkHttpClient =
        OkHttpClient
            .Builder()
            .connectTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(AccessTokenInterceptor(serverUrl = BuildConfig.API_BASE_URL, tokens = tokens))
            .build()

    /** Parser kontraktu. Nieznane pole odpowiedzi jest pomijane, bo serwer może dodać pola później. */
    @Provides
    @Singleton
    public fun json(): Json = Json { ignoreUnknownKeys = true }

    /**
     * Lista budynków, karta i podpowiedź punktu.
     *
     * Adres to [BuildConfig.API_BASE_URL]. Z emulatora `http://10.0.2.2:8080/` wskazuje serwer na komputerze
     * gospodarza.
     */
    @Provides
    @Singleton
    public fun buildingsApi(http: OkHttpClient, json: Json): BuildingsApi =
        BuildingsApi(http = http, json = json, baseUrl = BuildConfig.API_BASE_URL)

    /** Wysłanie i edycja zgłoszeń, zdjęcia i zgłoszenia przyjęte; ten sam adres co [buildingsApi]. */
    @Provides
    @Singleton
    public fun submissionsApi(http: OkHttpClient, json: Json): SubmissionsApi =
        SubmissionsApi(http = http, json = json, baseUrl = BuildConfig.API_BASE_URL)

    /** Wysyłka zmian znaczników piętra obejrzanego; ten sam adres co [buildingsApi]. */
    @Provides
    @Singleton
    public fun markerChangesApi(http: OkHttpClient, json: Json): MarkerChangesApi =
        MarkerChangesApi(http = http, json = json, baseUrl = BuildConfig.API_BASE_URL)

    /** Postęp inwentaryzacji i cofnięcie znacznika przez Koordynatora; ten sam adres co [buildingsApi]. */
    @Provides
    @Singleton
    public fun inspectionProgressApi(http: OkHttpClient, json: Json): InspectionProgressApi =
        InspectionProgressApi(http = http, json = json, baseUrl = BuildConfig.API_BASE_URL)

    /** Pobranie zmian z serwera, w tym znaczników pięter; ten sam adres co [buildingsApi]. */
    @Provides
    @Singleton
    public fun syncPullApi(http: OkHttpClient, json: Json): SyncPullApi =
        SyncPullApi(http = http, json = json, baseUrl = BuildConfig.API_BASE_URL)

    /** Zaproszenia Koordynatora; ten sam adres co [buildingsApi]. */
    @Provides
    @Singleton
    public fun invitationsApi(http: OkHttpClient, json: Json): InvitationsApi =
        InvitationsApi(http = http, json = json, baseUrl = BuildConfig.API_BASE_URL)
}
