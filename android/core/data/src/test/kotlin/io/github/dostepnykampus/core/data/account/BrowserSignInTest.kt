/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.account

import android.app.Application
import io.github.dostepnykampus.core.model.Account
import io.github.dostepnykampus.core.model.StaffRole
import io.github.dostepnykampus.core.network.auth.AccessTokens
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/**
 * Logowanie w przeglądarce: token z Usługi logowania ustawia konto, przerwanie zostawia Gościa.
 *
 * Wymagania: US14.2, BR-1, NFR-35
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BrowserSignInTest {
    private val app: Application = RuntimeEnvironment.getApplication()
    private val tokens = AccessTokens()
    private val session = sessionOf(tokens)
    private val signIn = BrowserSignIn(app, session)

    @Test
    fun signsInWithBrowserToken() =
        runTest {
            val result = async { signIn.open(SignInEntry.SIGN_IN) }
            runCurrent()
            val started = shadowOf(app).nextStartedActivity.component?.className
            assertEquals("aktywność logowania", LoginActivity::class.java.name, started)
            signIn.finish(loginWith(COLLECTOR_TOKEN))
            assertEquals("wynik", true, result.await())
            assertEquals("konto", Account.SignedIn(setOf(StaffRole.COLLECTOR)), session.account.value)
            assertEquals("token", COLLECTOR_TOKEN, tokens.token)
        }

    @Test
    fun staysGuestAfterCancel() =
        runTest {
            val result = async { signIn.open(SignInEntry.REGISTER) }
            runCurrent()
            signIn.finish(null)
            assertEquals("wynik", false, result.await())
            assertEquals("konto", Account.Guest, session.account.value)
        }

    @Test
    fun endsEarlierAttempt() =
        runTest {
            val first = async { signIn.open(SignInEntry.SIGN_IN) }
            runCurrent()
            val second = async { signIn.open(SignInEntry.SIGN_IN) }
            runCurrent()
            assertEquals("pierwsza próba", false, first.await())
            signIn.finish(loginWith(COLLECTOR_TOKEN))
            assertEquals("druga próba", true, second.await())
        }

    private companion object {
        val COLLECTOR_TOKEN = jwt("COLLECTOR")
    }
}
