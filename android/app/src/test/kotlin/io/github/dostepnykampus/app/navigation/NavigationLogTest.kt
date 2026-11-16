/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.navigation

import androidx.navigation3.runtime.NavKey
import io.github.dostepnykampus.app.TestRoles
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import timber.log.Timber

/**
 * Zmiana ekranu na wierzchu historii zostawia jedną linię logu z nazwą typu trasy, bez jej argumentów.
 *
 * Wymagania: NFR-57
 */
class NavigationLogTest {
    private val lines = mutableListOf<String>()
    private val tree =
        object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                lines += message
            }
        }

    private fun navigator() = AppNavigator(backStack = mutableListOf<NavKey>(MapRoute()), roles = TestRoles())

    @Test
    fun logsRouteNameWithoutArguments() {
        Timber.plant(tree)
        try {
            navigator().openBuilding("C-13")
        } finally {
            Timber.uproot(tree)
        }

        assertEquals(listOf("Ekran: BuildingRoute"), lines, "linia logu z nazwą trasy bez symbolu budynku")
    }
}
