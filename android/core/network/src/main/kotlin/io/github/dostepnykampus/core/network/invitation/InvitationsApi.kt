/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network.invitation

import io.github.dostepnykampus.core.model.StaffRole
import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.core.network.decodeContract
import io.github.dostepnykampus.core.network.decodedResult
import io.github.dostepnykampus.core.network.encodeContract
import io.github.dostepnykampus.core.network.exchange
import io.github.dostepnykampus.core.network.unexpectedResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.HttpURLConnection.HTTP_CREATED

/**
 * Wynik zaproszenia z serwera.
 *
 * @property isExistingAccount czy konto z tym adresem istniało; wtedy dostało rolę obok dotychczasowych
 */
public data class Invited(val isExistingAccount: Boolean)

/**
 * Klient HTTP zaproszeń Koordynatora: `POST /invitations`.
 *
 * Wynik to [ApiResult]: błąd sieci i zła treść nie wychodzą jako wyjątek; 400 przy złym adresie i 403 bez roli
 * Koordynatora to [ApiResult.ServerError] z tym kodem. Anulowanie korutyny przerywa żądanie HTTP.
 *
 * TODO(#1990): serwer nie ma jeszcze `POST /invitations`.
 *
 * Wymagania: US12.1, US12.2, BR-2
 *
 * @param http klient OkHttp
 * @param json parser JSON kontraktu
 * @param baseUrl adres serwera
 */
public class InvitationsApi(private val http: OkHttpClient, private val json: Json, baseUrl: String) {
    private val target =
        baseUrl
            .toHttpUrl()
            .newBuilder()
            .addPathSegment("invitations")
            .build()

    /**
     * Zaprasza osobę z adresem [email] i rolą [role]; konto, które już istnieje, dostaje tę rolę.
     *
     * @param email adres e-mail zapraszanej osoby
     * @param role rola pracownicza nadawana zaproszeniem
     */
    public suspend fun invite(email: String, role: StaffRole): ApiResult<Invited> {
        val payload =
            buildJsonObject {
                put(key = "email", value = email)
                put(key = "role", value = role.name)
            }
        val request =
            Request
                .Builder()
                .url(target)
                .header(name = "Accept", value = "application/json, application/problem+json")
                .post(json.encodeContract(JsonObject.serializer(), payload).toRequestBody(JSON))
                .build()
        return http.exchange(request) { code, body ->
            if (code == HTTP_CREATED) decodedResult(code) { invitedOf(body) } else unexpectedResult(code)
        }
    }

    private fun invitedOf(body: String): Invited {
        val existed = json.decodeContract(JsonElement.serializer(), body).jsonObject["accountExisted"]
        return Invited(isExistingAccount = requireNotNull(existed).jsonPrimitive.boolean)
    }

    private companion object {
        val JSON = "application/json".toMediaType()
    }
}
