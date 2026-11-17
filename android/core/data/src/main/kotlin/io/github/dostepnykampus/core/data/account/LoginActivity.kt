/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.account

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService

/**
 * Przezroczysta aktywność: otwiera stronę Usługi logowania, wymienia kod na token i oddaje wynik [BrowserSignIn].
 *
 * Wymagania: US14.2, NFR-31
 */
internal class LoginActivity : ComponentActivity() {
    private val signIn by lazy {
        EntryPointAccessors.fromApplication(applicationContext, LoginEntryPoint::class.java).browserSignIn()
    }
    private val service by lazy { AuthorizationService(this, loginConfiguration()) }
    private val browser =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { exchange(it.data) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val entry = SignInEntry.entries[intent.getIntExtra(ENTRY, 0)]
            browser.launch(service.getAuthorizationRequestIntent(loginRequest(entry)))
        }
    }

    override fun onDestroy() {
        service.dispose()
        super.onDestroy()
    }

    private fun exchange(data: Intent?) {
        val response = data?.let(AuthorizationResponse::fromIntent)
        if (response == null) {
            done(null)
        } else {
            service.performTokenRequest(response.createTokenExchangeRequest()) { tokens, _ ->
                done(tokens?.accessToken)
            }
        }
    }

    private fun done(token: String?) {
        signIn.finish(token)
        finish()
    }

    companion object {
        private const val ENTRY = "entry"

        /** Otwiera logowanie [entry] spoza aktywności. */
        fun intent(context: Context, entry: SignInEntry): Intent =
            Intent(context, LoginActivity::class.java)
                .putExtra(ENTRY, entry.ordinal)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

/** Dostęp [LoginActivity] do [BrowserSignIn] z grafu aplikacji. */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface LoginEntryPoint {
    /** Logowanie, które czeka na wynik aktywności. */
    fun browserSignIn(): BrowserSignIn
}
