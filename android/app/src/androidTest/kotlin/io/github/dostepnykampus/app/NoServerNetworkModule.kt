/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import android.graphics.Bitmap
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.github.dostepnykampus.core.network.BuildingsApi
import io.github.dostepnykampus.core.network.NetworkModule
import io.github.dostepnykampus.core.network.SubmissionsApi
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.ByteArrayOutputStream
import java.net.ConnectException
import javax.inject.Singleton

/**
 * Sieć testów na urządzeniu bez serwera: kafelki OSM zastępuje lokalny przezroczysty PNG (pusty podkład),
 * a pozostałe żądania kończą się błędem połączenia. Interceptor nie przekazuje żądań dalej. Klient MapLibre
 * dziedziczy go przez `newBuilder()`. Próba użycia DNS kończy test błędem, więc przejście testów nie zależy od
 * deklaracji braku sieci ani dostępności serwera. Parser i klienci API są te same co w [NetworkModule].
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [NetworkModule::class])
object NoServerNetworkModule {
    private const val TILE_SIZE = 256

    /** Klient HTTP, który nie otwiera połączeń. */
    @Provides
    @Singleton
    fun okHttp(): OkHttpClient {
        val tile = blankTile()
        return OkHttpClient
            .Builder()
            .dns { host -> error("Test próbuje użyć sieci: $host") }
            .addInterceptor(
                Interceptor { chain ->
                    val request = chain.request()
                    if (request.url.host != "tile.openstreetmap.org") {
                        throw ConnectException("Brak serwera w teście: ${request.url}")
                    }
                    LocalTileResponse(request, tile).response
                },
            )
            .build()
    }

    private fun blankTile(): ByteArray {
        val bitmap = Bitmap.createBitmap(TILE_SIZE, TILE_SIZE, Bitmap.Config.ARGB_8888)
        return try {
            ByteArrayOutputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
                output.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
    }

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

    /** Jedna odpowiedź ma własny strumień PNG; odbiorca zamyka go razem z [response], jak odpowiedź sieciową. */
    private class LocalTileResponse(request: Request, tile: ByteArray) {
        private val body = tile.toResponseBody("image/png".toMediaType())

        val response: Response =
            Response
                .Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("Lokalny kafelek testowy")
                .body(body)
                .build()
    }
}
