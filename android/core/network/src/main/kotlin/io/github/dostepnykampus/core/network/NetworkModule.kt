/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Klient HTTP `core:network` w grafie Hilt aplikacji (decyzja 97).
 *
 * Jedna instancja klienta, parsera i [BuildingsApi] na proces.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val CALL_TIMEOUT_SECONDS = 3L

    /**
     * Klient HTTP: całe wywołanie, z połączeniem i odczytem, trwa najwyżej 3 s. Mapa czeka na listę budynków
     * z serwera przed pierwszym pokazaniem, a bez serwera bierze plik konturów, więc nie może czekać dłużej
     * niż próg mapy z danymi w telefonie (NFR-10, NFR-11). Wysyłka zdjęć ustawi dłuższy limit na kopii klienta
     * z `newBuilder()`.
     */
    @Provides
    @Singleton
    fun okHttp(): OkHttpClient =
        OkHttpClient
            .Builder()
            .connectTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

    /** Parser kontraktu. Nieznane pole odpowiedzi jest pomijane, bo serwer może dodać pola później. */
    @Provides
    @Singleton
    fun json(): Json = Json { ignoreUnknownKeys = true }

    /**
     * Lista budynków, karta i podpowiedź punktu.
     *
     * Adres to [BuildConfig.API_BASE_URL]. Z emulatora `http://10.0.2.2:8080/` wskazuje serwer na komputerze
     * gospodarza.
     */
    @Provides
    @Singleton
    fun buildingsApi(http: OkHttpClient, json: Json): BuildingsApi =
        BuildingsApi(http = http, json = json, baseUrl = BuildConfig.API_BASE_URL)
}
