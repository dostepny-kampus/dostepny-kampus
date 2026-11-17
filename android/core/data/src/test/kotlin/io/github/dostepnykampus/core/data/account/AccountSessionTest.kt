/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.account

import io.github.dostepnykampus.core.model.Account
import io.github.dostepnykampus.core.model.StaffRole
import io.github.dostepnykampus.core.network.auth.AccessTokens
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * Sesja konta: rola konta z tokenu, wylogowanie do Gościa i token, którego nie da się odczytać.
 *
 * Wymagania: US14.1, BR-1, NFR-35
 */
class AccountSessionTest {
    @Test
    fun startsAsGuest() {
        val tokens = AccessTokens()
        assertEquals(Account.Guest, sessionOf(tokens).account.value, "konto na starcie")
        assertNull(tokens.token, "token na starcie")
    }

    @Test
    fun signsInWithRolesFromToken() {
        val tokens = AccessTokens()
        val session = sessionOf(tokens)
        val token = jwtOf("""{"realm_access":{"roles":["COLLECTOR","VERIFIER","offline_access"]}}""")
        assertTrue(session.signIn(token), "zalogowanie")
        val expected = Account.SignedIn(setOf(StaffRole.COLLECTOR, StaffRole.VERIFIER))
        assertEquals(expected, session.account.value, "role konta z tokenu")
        assertEquals(token, tokens.token, "token do żądań")
    }

    @ParameterizedTest
    @ValueSource(strings = ["""{"realm_access":{"roles":["offline_access"]}}""", """{"sub":"u-1"}"""])
    fun signsInVisitorWithoutRoles(claims: String) {
        val session = sessionOf()
        assertTrue(session.signIn(jwtOf(claims)), "zalogowanie")
        assertEquals(Account.SignedIn(), session.account.value, "Odwiedzający")
    }

    @Test
    fun signsOutToGuest() {
        val tokens = AccessTokens()
        val session = sessionOf(tokens)
        session.signIn(jwtOf("""{"realm_access":{"roles":["COORDINATOR"]}}"""))
        session.signOut()
        assertEquals(Account.Guest, session.account.value, "konto po wylogowaniu")
        assertNull(tokens.token, "token po wylogowaniu")
    }

    @ParameterizedTest
    @ValueSource(
        strings = ["", "to-nie-jest-token", "a.%%%.c", "a.bm90LWpzb24.c", "a.WzFd.c", "a.eyJyZWFsbV9hY2Nlc3MiOjF9.c"],
    )
    fun rejectsUnreadableToken(token: String) {
        val tokens = AccessTokens()
        val session = sessionOf(tokens)
        val before = jwtOf("""{"realm_access":{"roles":["VERIFIER"]}}""")
        session.signIn(before)
        assertFalse(session.signIn(token), "token nie do odczytania")
        assertEquals(Account.SignedIn(setOf(StaffRole.VERIFIER)), session.account.value, "konto bez zmian")
        assertEquals(before, tokens.token, "token bez zmian")
    }
}
