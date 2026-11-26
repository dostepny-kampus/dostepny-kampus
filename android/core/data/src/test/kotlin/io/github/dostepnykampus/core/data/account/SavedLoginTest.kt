/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.account

import io.github.dostepnykampus.core.model.Account
import io.github.dostepnykampus.core.model.StaffRole
import io.github.dostepnykampus.core.network.auth.AccessTokens
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Zalogowanie pamiętane po zamknięciu aplikacji: zapis, powrót z zapisu i odświeżenie tokenu.
 *
 * Wymagania: US14.4, NFR-35
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SavedLoginTest {
    private val file = File.createTempFile("logowanie", null).apply { delete() }
    private val hourLater = System.currentTimeMillis() + HOUR_MS
    private val collector = Account.SignedIn(setOf(StaffRole.COLLECTOR))

    /**
     * Po ponownym uruchomieniu konto wraca z zapisu.
     * Scenariusz (częściowo): uprawnienia.feature: Zalogowany Odwiedzający nie jest proszony o logowanie ponownie
     */
    @Test
    fun restoresAccountAfterRestart() =
        runTest {
            val work = UnconfinedTestDispatcher(testScheduler)
            val token = jwt("COLLECTOR")
            sessionOf(file = file, work = work).signIn(loginWith(token, hourLater))
            assertFalse("token jawnym tekstem w zapisie", file.readText().contains(token))
            val tokens = AccessTokens()
            val restored = sessionOf(tokens, file, work = work)
            assertEquals("konto po ponownym uruchomieniu", collector, restored.account.value)
            assertEquals("token do żądań", token, tokens.token)
        }

    /**
     * Odrzucone odświeżenie logowania zostawia Gościa i usuwa zapis.
     * Scenariusz (częściowo): uprawnienia.feature: Zalogowany Odwiedzający nie jest proszony o logowanie ponownie
     */
    @Test
    fun forgetsRejectedLogin() =
        runTest {
            val work = UnconfinedTestDispatcher(testScheduler)
            LoginVault(file, ReversingCipher).write(loginWith(jwt("COLLECTOR"), expiresAt = 0L))
            val tokens = AccessTokens()
            val session = sessionOf(tokens = tokens, file = file, refresher = { Renewal.Rejected }, work = work)
            runCurrent()
            assertEquals("konto po odrzuconym odświeżeniu", Account.Guest, session.account.value)
            assertNull("token po odrzuconym odświeżeniu", tokens.token)
            assertFalse("zapis po odrzuconym odświeżeniu", file.exists())
        }

    @Test
    fun refreshesBeforeExpiry() =
        runTest {
            val work = UnconfinedTestDispatcher(testScheduler)
            val fresh = jwt("VERIFIER")
            sessionOf(file = file, work = work).signIn(loginWith(jwt("COLLECTOR"), expiresAt = 0L))
            val session = sessionOf(file = file, refresher = { Renewal.Fresh(loginWith(fresh)) }, work = work)
            runCurrent()
            assertEquals("konto po odświeżeniu", Account.SignedIn(setOf(StaffRole.VERIFIER)), session.account.value)
            assertEquals("zapis po odświeżeniu", fresh, LoginVault(file, ReversingCipher).read()?.accessToken)
        }

    @Test
    fun retriesWhenServiceUnreachable() =
        runTest {
            val work = UnconfinedTestDispatcher(testScheduler)
            val answers = ArrayDeque(listOf(Renewal.Unreachable, Renewal.Rejected))
            LoginVault(file, ReversingCipher).write(loginWith(jwt("COLLECTOR"), expiresAt = 0L))
            val session = sessionOf(file = file, refresher = { answers.removeFirst() }, work = work)
            runCurrent()
            assertEquals("konto, gdy usługa nieosiągalna", collector, session.account.value)
            advanceTimeBy(RETRY_MS)
            runCurrent()
            assertEquals("konto po ponowieniu", Account.Guest, session.account.value)
        }

    @Test
    fun forgetsLoginAfterSignOut() =
        runTest {
            val work = UnconfinedTestDispatcher(testScheduler)
            val session = sessionOf(file = file, work = work)
            session.signIn(loginWith(jwt("COLLECTOR"), hourLater))
            session.signOut()
            assertFalse("zapis po wylogowaniu", file.exists())
            assertEquals("konto po ponownym uruchomieniu", Account.Guest, sessionOf(file = file).account.value)
        }

    private companion object {
        const val HOUR_MS = 3_600_000L
        const val RETRY_MS = 30_000L
    }
}
