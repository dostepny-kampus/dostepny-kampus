/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.model

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AccountTest {
    @Test
    fun us14_2_guest_not_signed_in() {
        assertFalse(Account.Guest.isSignedIn)
        assertFalse(Account.Guest.hasStaffRole)
    }

    @Test
    fun us14_1_visitor_no_staff_role() {
        assertTrue(Account.SignedIn().isSignedIn)
        assertFalse(Account.SignedIn().hasStaffRole)
    }

    @Test
    fun br1_any_staff_role_counts() {
        StaffRole.entries.forEach { assertTrue(Account.SignedIn(setOf(it)).hasStaffRole) }
    }
}
