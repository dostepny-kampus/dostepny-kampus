/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.account

import android.net.Uri
import androidx.core.net.toUri
import io.github.dostepnykampus.core.data.BuildConfig
import net.openid.appauth.AppAuthConfiguration
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import java.net.HttpURLConnection
import java.net.URI

internal const val LOGIN_CLIENT_ID = "dostepny-kampus-android"
internal const val LOGIN_REDIRECT = "io.github.dostepnykampus:/oauth2redirect"
private const val REGISTER_PROMPT = "create"

/**
 * Żądanie Authorization Code z PKCE S256 do realmu [realm] dla klienta publicznego aplikacji.
 *
 * Wymagania: US14.2, US14.3, NFR-31
 *
 * @param entry logowanie albo rejestracja (`prompt=create`)
 * @param realm adres realmu Usługi logowania zakończony ukośnikiem
 */
internal fun loginRequest(entry: SignInEntry, realm: Uri = BuildConfig.LOGIN_REALM_URL.toUri()): AuthorizationRequest {
    val endpoints = AuthorizationServiceConfiguration(realm.endpoint("auth"), realm.endpoint("token"))
    return AuthorizationRequest
        .Builder(endpoints, LOGIN_CLIENT_ID, ResponseTypeValues.CODE, LOGIN_REDIRECT.toUri())
        .setScope("openid")
        .setPrompt(REGISTER_PROMPT.takeIf { entry == SignInEntry.REGISTER })
        .build()
}

/** Połączenia AppAuth z realmem z konfiguracji budowy; w debug realm deweloperski bez HTTPS. */
internal fun loginConfiguration(): AppAuthConfiguration =
    AppAuthConfiguration
        .Builder()
        .setConnectionBuilder { uri -> URI(uri.toString()).toURL().openConnection() as HttpURLConnection }
        .setSkipIssuerHttpsCheck(BuildConfig.DEBUG)
        .build()

private fun Uri.endpoint(name: String): Uri = buildUpon().appendEncodedPath("protocol/openid-connect/$name").build()
