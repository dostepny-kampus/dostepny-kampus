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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.capture.location.LocationPermissionContent
import io.github.dostepnykampus.feature.capture.location.LocationPermissionState
import io.github.dostepnykampus.feature.capture.location.WithLocationPermission
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Prośba o zgodę na lokalizację przed pomiarem C1. Wymagania: US6.1, NFR-27, BR-8 */
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

    private fun show(
        state: () -> LocationPermissionState,
        onAllow: () -> Unit = {},
        onOpenSettings: () -> Unit = {},
    ) {
        compose.setContent {
            DostepnyKampusTheme {
                LocationPermissionContent(
                    state = state(),
                    onAllow = onAllow,
                    onOpenSettings = onOpenSettings,
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun asksWithoutPermission() {
        shadowOf(app).denyPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        gate()
        compose.onNodeWithText(MISSING_TEXT).assertIsDisplayed()
        compose.onNodeWithText("Pozwól na lokalizację").assertIsDisplayed()
        compose.onNodeWithText("Pomiar").assertDoesNotExist()
    }

    @Test
    fun asksPreciseWithCoarseOnly() {
        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
        shadowOf(app).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        gate()
        compose.onNodeWithText(APPROXIMATE_TEXT).assertIsDisplayed()
        compose.onNodeWithText("Pozwól na dokładną lokalizację").assertIsDisplayed()
        compose.onNodeWithText("Pomiar").assertDoesNotExist()
    }

    @Test
    fun measuresWithPermission() {
        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        gate()
        compose.onNodeWithText("Pomiar").assertIsDisplayed()
    }

    /** Po odmowie „Otwórz ustawienia” zamiast ponownego pytania. */
    @Test
    fun deniedOpensSettings() {
        var opened = 0
        show(state = { LocationPermissionState.DENIED }, onOpenSettings = { opened++ })
        compose.onNodeWithText(DENIED_TEXT).assertIsDisplayed()
        compose.onNodeWithText("Otwórz ustawienia").performClick()
        assertEquals("otwiera ustawienia aplikacji po odmowie", 1, opened)
    }

    @Test
    fun allowsFromMissingAndApprox() {
        var allows = 0
        var state by mutableStateOf(LocationPermissionState.MISSING)
        show(state = { state }, onAllow = { allows++ })
        compose.onNodeWithText(MISSING_TEXT).assertIsDisplayed()
        compose.onNodeWithText("Pozwól na lokalizację").performClick()
        assertEquals("prosi o lokalizację przy braku zgody", 1, allows)

        state = LocationPermissionState.APPROXIMATE
        compose.onNodeWithText(APPROXIMATE_TEXT).assertIsDisplayed()
        compose.onNodeWithText("Pozwól na dokładną lokalizację").performClick()
        assertEquals("prosi o dokładną przy zgodzie przybliżonej", 2, allows)
    }

    private companion object {
        const val MISSING_TEXT =
            "Aby zapisać miejsce bariery, potrzebujemy lokalizacji. " +
                "Sprawdzimy ją tylko teraz i nie będziemy Cię śledzić."

        const val APPROXIMATE_TEXT =
            "Telefon podaje tylko przybliżone miejsce, a to za mało, by wskazać barierę. " +
                "Pozwól na dokładną lokalizację."

        const val DENIED_TEXT =
            "Bez lokalizacji nie zapiszesz miejsca bariery. Możesz ją włączyć w ustawieniach telefonu."
    }
}
