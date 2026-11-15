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
        assertFalse(Account.Guest.isSignedIn, "gość nie jest zalogowany")
        assertFalse(Account.Guest.hasStaffRole, "gość nie ma roli pracownika")
    }

    @Test
    fun givesVisitorNoStaffRole() {
        assertTrue(Account.SignedIn().isSignedIn, "zalogowany bez ról jest zalogowany")
        assertFalse(Account.SignedIn().hasStaffRole, "zalogowany bez ról nie ma roli pracownika")
    }

    @Test
    fun countsAnyStaffRole() {
        StaffRole.entries.forEach {
            assertTrue(Account.SignedIn(setOf(it)).hasStaffRole, "każda rola pracownika daje rolę pracownika")
        }
    }
}
