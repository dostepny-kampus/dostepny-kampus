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
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Zalogowanie pamiętane po zamknięciu aplikacji: zapis, powrót z zapisu i wylogowanie.
 *
 * Wymagania: US14.4, NFR-35
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SavedLoginTest {
    private val file = File.createTempFile("logowanie", null).apply { delete() }
    private val hourLater = System.currentTimeMillis() + HOUR_MS
    private val collector = Account.SignedIn(setOf(StaffRole.COLLECTOR))

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
    }
}
