/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.dostepnykampus.core.model.StaffRole
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Każda rola pracownicza ma nazwę ze słownika użytkownika. Wymagania: BR-1, BR-5 */
@RunWith(RobolectricTestRunner::class)
class StaffRoleNamesTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun rolesHaveNames() {
        assertEquals(
            "nazwy ról",
            listOf("Zbierający", "Weryfikator", "Koordynator"),
            StaffRole.entries.map { context.getString(it.nameRes()) },
        )
    }
}
