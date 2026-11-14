/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED

/**
 * Nagłówek `Authorization: Bearer` z [tokens] w żądaniach do serwera aplikacji pod [serverUrl]. Operacje publiczne
 * kontraktu (`/buildings…`, `/rooms`) i inne adresy, np. kafelki mapy, idą bez tokenu. Odpowiedź 401 serwera
 * aplikacji trafia do [AccessTokens.rejections].
 *
 * @param serverUrl adres serwera aplikacji, np. `http://10.0.2.2:8080/`
 * @param tokens token konta
 */
internal class AccessTokenInterceptor(serverUrl: String, private val tokens: AccessTokens) : Interceptor {
    private val server = serverUrl.toHttpUrl()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val isServer = request.url.isOnServer()
        val token = tokens.token?.takeIf { isServer && !request.url.isPublic() }
        val sent =
            if (token == null) {
                request
            } else {
                request.newBuilder().header(name = AUTHORIZATION, value = "$BEARER $token").build()
            }
        return chain.proceed(sent).also { response ->
            if (isServer && response.code == HTTP_UNAUTHORIZED) tokens.reject()
        }
    }

    private fun HttpUrl.isOnServer(): Boolean = toString().startsWith(server.toString())

    private fun HttpUrl.isPublic(): Boolean = pathSegments.getOrNull(server.pathSegments.size - 1) in PUBLIC

    private companion object {
        const val AUTHORIZATION = "Authorization"
        const val BEARER = "Bearer"

        val PUBLIC = setOf("buildings", "rooms")
    }
}
