/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.github.dostepnykampus.core.network.BuildingsApi
import io.github.dostepnykampus.core.network.NetworkModule
import io.github.dostepnykampus.core.network.SubmissionsApi
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.net.ConnectException
import javax.inject.Singleton

/**
 * Sieć testów na urządzeniu bez serwera: każde żądanie HTTP od razu kończy się błędem połączenia, bez wyjścia
 * z emulatora i bez czekania na limit czasu klienta. Wynik nie zależy od tego, czy coś słucha pod adresem serwera
 * na komputerze gospodarza. Parser i klienci API są te same co w [NetworkModule].
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [NetworkModule::class])
object NoServerNetworkModule {
    /** Klient HTTP, który nie otwiera połączeń. */
    @Provides
    @Singleton
    fun okHttp(): OkHttpClient =
        OkHttpClient
            .Builder()
            .addInterceptor { chain -> throw ConnectException("Brak serwera w teście: ${chain.request().url}") }
            .build()

    /** Parser kontraktu z [NetworkModule.json]. */
    @Provides
    @Singleton
    fun json(): Json = NetworkModule.json()

    /** Lista budynków i karta na kliencie bez serwera. */
    @Provides
    @Singleton
    fun buildingsApi(http: OkHttpClient, json: Json): BuildingsApi = NetworkModule.buildingsApi(http, json)

    /** Zgłoszenia na kliencie bez serwera. */
    @Provides
    @Singleton
    fun submissionsApi(http: OkHttpClient, json: Json): SubmissionsApi = NetworkModule.submissionsApi(http, json)
}
