/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.model

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Konto i role: gość, odwiedzający, pracownik.
 *
 * Wymagania: US14.2, US14.1, BR-1
 */
class AccountTest {
    @Test
    fun keepsGuestSignedOut() {
        assertFalse(Account.Guest.isSignedIn)
        assertFalse(Account.Guest.hasStaffRole)
    }

    @Test
    fun givesVisitorNoStaffRole() {
        assertTrue(Account.SignedIn().isSignedIn)
        assertFalse(Account.SignedIn().hasStaffRole)
    }

    @Test
    fun countsAnyStaffRole() {
        StaffRole.entries.forEach { assertTrue(Account.SignedIn(setOf(it)).hasStaffRole) }
    }
}
