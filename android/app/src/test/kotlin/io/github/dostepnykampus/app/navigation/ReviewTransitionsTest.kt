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
import io.github.dostepnykampus.feature.review.navigation.ReviewCheckRoute
import io.github.dostepnykampus.feature.review.navigation.ReviewQueueRoute
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

/**
 * Przejścia Weryfikatora: L2 → R1 → R2 i z powrotem.
 *
 * Wymagania: US10.4, US10.7
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

    /** Wiersz: R1 „Weź do sprawdzenia” albo „Otwórz” → R2, „Wstecz” → R1. Wymagania: US10.7, BR-11 */
    @Test
    fun checkOpensFromQueue() {
        val navigator = navigator()
        navigator.reviewQueue()
        navigator.openReviewCheck(ID)
        assertEquals(ReviewCheckRoute(ID), stack.last(), "R2 nad R1")
        navigator.openReviewCheck("b")
        assertEquals(ReviewCheckRoute(ID), stack.last(), "R2 nie otwiera drugiego R2")
        navigator.back()
        assertEquals(ReviewQueueRoute, stack.last(), "powrót do R1")
    }

    /** Wiersz: decyzja na R2 → R1 ze świeżą kolejką, bez R2 w historii. Wymagania: US10, NFR-54 */
    @Test
    fun decisionRestartsQueue() {
        val navigator = navigator()
        navigator.reviewQueue()
        val queue = navigator.contentKey(stack.lastIndex)
        navigator.openReviewCheck(ID)
        navigator.reviewDecided()
        assertEquals(listOf(MapRoute(), AccountRoute, ReviewQueueRoute), stack, "stos po decyzji")
        assertNotEquals(queue, navigator.contentKey(stack.lastIndex), "R1 w świeżym stanie")
    }

    private companion object {
        const val ID = "a"
    }
}
