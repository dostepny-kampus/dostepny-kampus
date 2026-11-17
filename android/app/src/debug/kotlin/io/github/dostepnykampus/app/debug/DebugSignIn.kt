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
import io.github.dostepnykampus.core.data.account.AccountSession
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

/**
 * Logowanie użytkownikiem testowym roli wybranej w przełączniku debug. Token z realmu
 * deweloperskiego trafia tylko do [AccountSession] w pamięci procesu, a rola konta wynika z ról zapisanych w tokenie.
 * Ani token, ani hasło użytkownika testowego (`infra/keycloak/realm.json`) nie trafiają do logów.
 *
 * Wymagania: US14.1, BR-1, NFR-35
 *
 * @param session sesja konta pokazywana przez aplikację
 * @param http klient HTTP
 * @param tokenUrl adres wydawania tokenów realmu, np. z emulatora `http://10.0.2.2:8180/realms/…/token`
 * @param io wątki do żądania HTTP
 */
class DebugSignIn(
    private val session: AccountSession,
    private val http: OkHttpClient,
    private val tokenUrl: String,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    /**
     * Gość usuwa token; inna rola loguje jej użytkownika testowego. Błąd logowania nie zmienia roli ani tokenu.
     *
     * @return `true`, gdy konto pasuje już do wybranej roli
     */
    suspend fun choose(role: DebugRole): Boolean {
        val user = USERS[role]
        if (user == null) {
            session.signOut()
            return true
        }
        val token = withContext(io) { fetch(login = user.first, password = user.second) }
        return token != null && session.signIn(token)
    }

    private fun fetch(login: String, password: String): String? {
        val form =
            FormBody
                .Builder()
                .add(name = "grant_type", value = "password")
                .add(name = "client_id", value = CLIENT_ID)
                .add(name = "username", value = login)
                .add(name = "password", value = password)
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
        val USERS =
            mapOf(
                DebugRole.VISITOR to ("odwiedzajacy.test" to "haslo-testowe-odwiedzajacy"),
                DebugRole.COLLECTOR to ("zbierajacy.test" to "haslo-testowe-zbierajacy"),
                DebugRole.VERIFIER to ("weryfikator.test" to "haslo-testowe-weryfikator"),
                DebugRole.COORDINATOR to ("koordynator.test" to "haslo-testowe-koordynator"),
            )
    }
}

/** Logowanie debug w grafie Hilt, z adresem realmu deweloperskiego z `infra/` widzianym z emulatora. */
@Module
@InstallIn(SingletonComponent::class)
object DebugSignInModule {
    private const val TOKEN_URL = "http://10.0.2.2:8180/realms/dostepny-kampus/protocol/openid-connect/token"

    /** Logowanie z tą samą [AccountSession] co reszta aplikacji. */
    @Provides
    fun signIn(session: AccountSession, http: OkHttpClient): DebugSignIn =
        DebugSignIn(session = session, http = http, tokenUrl = TOKEN_URL)
}
