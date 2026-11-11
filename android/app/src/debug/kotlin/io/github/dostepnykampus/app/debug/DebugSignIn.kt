/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.debug

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.dostepnykampus.core.data.account.TymczasoweRole
import io.github.dostepnykampus.core.model.Account
import io.github.dostepnykampus.core.model.StaffRole
import io.github.dostepnykampus.core.network.AccessTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.Base64

/**
 * Logowanie użytkownikiem testowym roli wybranej w przełączniku debug (US14.1, BR-1). Token z realmu
 * deweloperskiego trafia tylko do [AccessTokens] w pamięci procesu, a rola konta wynika z ról zapisanych w tokenie.
 * Ani token, ani hasło nie trafiają do logów (NFR-35).
 *
 * @param roles konto pokazywane przez aplikację
 * @param tokens token dołączany do żądań do serwera
 * @param http klient HTTP
 * @param tokenUrl adres wydawania tokenów realmu, np. z emulatora `http://10.0.2.2:8180/realms/…/token`
 */
@TymczasoweRole
class DebugSignIn(
    private val roles: DebugRoles,
    private val tokens: AccessTokens,
    private val http: OkHttpClient,
    private val tokenUrl: String,
) {
    /**
     * Gość usuwa token; inna rola loguje jej użytkownika testowego. Błąd logowania nie zmienia roli ani tokenu.
     *
     * @return `true`, gdy konto pasuje już do wybranej roli
     */
    suspend fun choose(role: DebugRole): Boolean {
        val user = role.user
        if (user == null) {
            tokens.signOut()
            roles.account = Account.Guest
            return true
        }
        val token = withContext(Dispatchers.IO) { fetch(user) } ?: return false
        val account = accountOf(token) ?: return false
        tokens.signIn(token)
        roles.account = account
        return true
    }

    private fun fetch(user: DebugUser): String? {
        val form =
            FormBody
                .Builder()
                .add(name = "grant_type", value = "password")
                .add(name = "client_id", value = CLIENT_ID)
                .add(name = "username", value = user.login)
                .add(name = "password", value = user.password)
                .build()
        val request =
            Request
                .Builder()
                .url(tokenUrl)
                .post(form)
                .build()
        val call = http.newCall(request)
        val token =
            try {
                call.execute().use { response ->
                    if (response.isSuccessful) accessToken(response.body.string()) else null
                }
            } catch (_: IOException) {
                null
            }
        return token?.takeIf { it.isNotEmpty() }
    }

    private fun accessToken(body: String): String? =
        try {
            JSONObject(body).optString(ACCESS_TOKEN)
        } catch (_: JSONException) {
            null
        }

    private companion object {
        const val CLIENT_ID = "dostepny-kampus-android"
        const val ACCESS_TOKEN = "access_token"
    }
}

/**
 * Konto z ról realmu w tokenie (`realm_access.roles`) albo `null`, gdy tokenu nie da się odczytać.
 *
 * @param token token JWT z Keycloak
 */
internal fun accountOf(token: String): Account? {
    val payload = token.split('.').getOrNull(1) ?: return null
    return try {
        val claims = JSONObject(String(Base64.getUrlDecoder().decode(payload), Charsets.UTF_8))
        val granted = claims.optJSONObject("realm_access")?.optJSONArray("roles")
        val names = granted?.let { array -> List(array.length()) { array.optString(it) } }.orEmpty()
        Account.SignedIn(StaffRole.entries.filter { it.name in names }.toSet())
    } catch (_: IllegalArgumentException) {
        null
    } catch (_: JSONException) {
        null
    }
}

/** Logowanie debug w grafie Hilt, z adresem realmu deweloperskiego z `infra/` widzianym z emulatora. */
@Module
@InstallIn(SingletonComponent::class)
object DebugSignInModule {
    private const val TOKEN_URL = "http://10.0.2.2:8180/realms/dostepny-kampus/protocol/openid-connect/token"

    /** Logowanie z tym samym [DebugRoles] i [AccessTokens] co reszta aplikacji. */
    @OptIn(TymczasoweRole::class)
    @Provides
    fun signIn(roles: DebugRoles, tokens: AccessTokens, http: OkHttpClient): DebugSignIn =
        DebugSignIn(roles = roles, tokens = tokens, http = http, tokenUrl = TOKEN_URL)
}
