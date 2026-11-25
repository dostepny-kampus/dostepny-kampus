/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.navigation

import androidx.navigation3.runtime.NavKey
import io.github.dostepnykampus.app.TestRoles
import io.github.dostepnykampus.core.model.Account
import io.github.dostepnykampus.core.model.StaffRole
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import io.github.dostepnykampus.feature.review.navigation.ReviewQueueRoute
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Przejścia Weryfikatora z L2 do kolejki weryfikacji i z powrotem.
 *
 * Wymagania: US10.4
 */
class ReviewTransitionsTest {
    private val stack = mutableListOf<NavKey>(MapRoute(), AccountRoute)
    private val verifier = Account.SignedIn(setOf(StaffRole.VERIFIER))

    private fun navigator() = AppNavigator(backStack = stack, roles = TestRoles(verifier))

    /** Wiersz: L2 „Zgłoszenia do sprawdzenia” → R1, „Wstecz” → L2. Wymagania: US10.4, RO-WER */
    @Test
    fun queueOpensAndGoesBack() {
        navigator().reviewQueue()
        assertEquals(listOf(MapRoute(), AccountRoute, ReviewQueueRoute), stack, "stos po wejściu do R1")
        navigator().back()
        assertEquals(listOf(MapRoute(), AccountRoute), stack, "stos po „Wstecz”")
    }
}
