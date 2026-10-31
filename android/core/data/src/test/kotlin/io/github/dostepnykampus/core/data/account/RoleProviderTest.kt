/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.account

import io.github.dostepnykampus.core.model.Account
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RoleProviderTest {
    // RO-GOS, US1.2: bez logowania w aplikacji każda osoba jest Gościem niezalogowanym (mapa nawigacji, rola Gość).
    @Test
    fun ro_gos_everyone_is_guest() {
        assertEquals(Account.Guest, GuestRoleProvider.currentAccount())
    }
}
