/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.systemtest

import io.github.dostepnykampus.core.network.NetworkModule
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import java.net.HttpURLConnection.HTTP_OK

/** Pobiera token testowego Zbierającego z realmu deweloperskiego bez zapisu danych logowania. */
internal fun collectorToken(): String {
    val form =
        FormBody
            .Builder()
            .add(name = "grant_type", value = "password")
            .add(name = "client_id", value = "dostepny-kampus-android")
            .add(name = "username", value = "zbierajacy.test")
            .add(name = "password", value = "haslo-testowe-zbierajacy")
            .build()
    val request =
        Request
            .Builder()
            .url("http://10.0.2.2:8180/realms/dostepny-kampus/protocol/openid-connect/token")
            .post(form)
            .build()
    val http = OkHttpClient()
    return http.newCall(request).execute().use { response ->
        assertEquals("Logowanie Zbierającego", HTTP_OK, response.code)
        val fields = NetworkModule.json().parseToJsonElement(response.body.string()).jsonObject
        fields.getValue("access_token").jsonPrimitive.content
    }
}
