/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import android.Manifest
import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Prośba o zgodę na lokalizację przed pomiarem C1. Wymagania: US6.1, NFR-27 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class LocationPermissionTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app = ApplicationProvider.getApplicationContext<Application>()

    private fun gate() {
        compose.setContent {
            DostepnyKampusTheme { WithLocationPermission(onBack = {}) { Text("Pomiar") } }
        }
    }

    @Test
    fun nfr27_asks_without_permission() {
        shadowOf(app).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        gate()
        compose.onNodeWithText("Zezwól na lokalizację").assertIsDisplayed()
        compose.onNodeWithText("Pomiar").assertDoesNotExist()
    }

    @Test
    fun us6_1_measures_with_permission() {
        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        gate()
        compose.onNodeWithText("Pomiar").assertIsDisplayed()
    }

    /** Po odmowie „Otwórz ustawienia” zamiast ponownego pytania. */
    @Test
    fun nfr27_denied_opens_settings() {
        var opened = 0
        compose.setContent {
            DostepnyKampusTheme {
                LocationPermissionContent(isDenied = true, onAllow = {}, onOpenSettings = { opened++ }, onBack = {})
            }
        }
        compose.onNodeWithText("Bez zgody", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Otwórz ustawienia").performClick()
        assertEquals(1, opened)
    }
}
