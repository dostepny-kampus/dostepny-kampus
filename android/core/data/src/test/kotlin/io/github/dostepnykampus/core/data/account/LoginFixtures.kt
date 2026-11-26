/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.account

import androidx.core.net.toUri
import io.github.dostepnykampus.core.network.auth.AccessTokens
import kotlinx.coroutines.Dispatchers
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.GrantTypeValues
import net.openid.appauth.TokenRequest
import net.openid.appauth.TokenResponse
import java.io.File
import java.util.Base64
import kotlin.coroutines.CoroutineContext

/** Token JWT z rolami realmu [roles], bez prawdziwego podpisu. */
internal fun jwt(vararg roles: String): String {
    val names = roles.joinToString(separator = ",") { name -> "\"$name\"" }
    return jwtOf("""{"realm_access":{"roles":[$names]}}""")
}

/** Token JWT z treścią [claims], bez prawdziwego podpisu. */
internal fun jwtOf(claims: String): String {
    val encoder = Base64.getUrlEncoder().withoutPadding()
    val parts = listOf("""{"alg":"RS256"}""", claims, "podpis")
    return parts.joinToString(separator = ".") { part -> encoder.encodeToString(part.toByteArray()) }
}

/** Stan logowania z tokenem dostępu [token] ważnym do [expiresAt] i tokenem odświeżania. */
internal fun loginWith(token: String, expiresAt: Long? = null): AuthState {
    val configuration =
        AuthorizationServiceConfiguration(
            "https://logowanie.test/auth".toUri(),
            "https://logowanie.test/token".toUri(),
        )
    val request =
        TokenRequest
            .Builder(configuration, LOGIN_CLIENT_ID)
            .setGrantType(GrantTypeValues.REFRESH_TOKEN)
            .setRefreshToken(REFRESH_TOKEN)
            .build()
    val tokens =
        TokenResponse
            .Builder(request)
            .setTokenType(TokenResponse.TOKEN_TYPE_BEARER)
            .setAccessToken(token)
            .setRefreshToken(REFRESH_TOKEN)
            .setAccessTokenExpirationTime(expiresAt)
            .build()
    return AuthState(configuration).apply { update(tokens, null) }
}

/** Szyfr testowy: odwraca bajty, więc zapis nie zawiera tokenu jawnym tekstem. */
internal object ReversingCipher : LoginCipher {
    override fun seal(plain: ByteArray): ByteArray = plain.reversedArray()

    override fun open(sealed: ByteArray): ByteArray = sealed.reversedArray()
}

/** Sesja z zapisem w [file] i odświeżeniem [refresher]. */
internal fun sessionOf(
    tokens: AccessTokens = AccessTokens(),
    file: File = File.createTempFile("logowanie", null).apply { delete() },
    refresher: LoginRefresh = LoginRefresh { Renewal.Rejected },
    work: CoroutineContext = Dispatchers.Unconfined,
    onLogin: () -> Unit = {},
): AccountSession =
    AccountSession(
        tokens = tokens,
        vault = LoginVault(file, ReversingCipher),
        refresher = refresher,
        work = work,
        queue =
            object : QueueHandover {
                override suspend fun adopt(subject: String) = Unit

                override fun onLoggedIn() = onLogin()
            },
    )

internal const val REFRESH_TOKEN = "odswiezenie"
