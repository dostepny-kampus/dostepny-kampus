/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.systemtest

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

private const val TOKEN_URL = "http://10.0.2.2:8180/realms/dostepny-kampus/protocol/openid-connect/token"

/**
 * Token dostępu użytkownika testowego [login] z realmu deweloperskiego widzianego z emulatora, wydany klientowi
 * aplikacji bez przeglądarki logowania.
 *
 * Wymagania: US14.1
 *
 * @param login nazwa użytkownika testowego, np. `zbierajacy.test`
 * @param password hasło użytkownika testowego z `infra/keycloak/realm.json`
 */
internal fun realmToken(login: String, password: String): String {
    val form =
        FormBody
            .Builder()
            .add(name = "grant_type", value = "password")
            .add(name = "client_id", value = "dostepny-kampus-android")
            .add(name = "username", value = login)
            .add(name = "password", value = password)
            .build()
    val request =
        Request
            .Builder()
            .url(TOKEN_URL)
            .post(form)
            .build()
    val body = OkHttpClient().newCall(request).execute().use { it.body.string() }
    return JSONObject(body).getString("access_token")
}
