/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.quick

import android.Manifest
import android.app.Application
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.app.ActivityOptionsCompat
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.location.LocationSwitch
import io.github.dostepnykampus.core.location.PositionSource
import io.github.dostepnykampus.feature.capture.navigation.ReportRoute
import io.github.dostepnykampus.feature.capture.photo.PhotoTranscoder
import io.github.dostepnykampus.feature.capture.report.ReportSaver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Zgoda na lokalizację na ekranie szybkiego zgłoszenia. Wymagania: UC7, NFR-27, BR-8, BR-14 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class QuickLocationPermissionTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val locationOn = MutableStateFlow(true)
    private val permissionOwner = DeniedResultOwner()
    private var mapOpens = 0

    @Test
    fun asksForLocationBeforeCamera() {
        show()

        expectNotice(message = "Aby wysłać zdjęcie bariery, potrzebujemy jej miejsca.", action = ALLOW)
        compose.onNodeWithText("Sprawdzimy je tylko teraz i nie będziemy Cię śledzić.").assertIsDisplayed()
        compose.onRoot().captureRoboImage()
        compose.onNodeWithText(BACK_TO_MAP).performClick()
        assertEquals("wraca na mapę bez zdjęcia", 1, mapOpens)
    }

    @Test
    fun asksForPreciseWithCoarse() {
        RuntimeEnvironment.setFontScale(2f)
        show(Manifest.permission.ACCESS_COARSE_LOCATION)

        expectNotice(
            message = "Telefon podaje tylko przybliżone miejsce, a to za mało, by wskazać barierę.",
            action = "Pozwól na dokładną lokalizację",
        )
        compose.onNodeWithText(BACK_TO_MAP).performScrollTo().assertIsDisplayed()
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun opensAppSettingsAfterDenial() {
        show()

        compose.onNodeWithText(ALLOW).performClick()
        expectNotice(message = "Bez lokalizacji nie wyślesz zdjęcia bariery.", action = SETTINGS)
        compose.onNodeWithText("Możesz ją włączyć w ustawieniach telefonu.").assertIsDisplayed()
        compose.onNodeWithText(SETTINGS).performClick()
        val opened = shadowOf(compose.activity).nextStartedActivity
        assertEquals(
            "po odmowie otwiera ustawienia aplikacji",
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            opened.action,
        )
    }

    @Test
    fun resumesAfterEnablingLocation() {
        locationOn.value = false
        show(Manifest.permission.ACCESS_FINE_LOCATION)

        expectNotice(message = "Lokalizacja w telefonie jest wyłączona.", action = SETTINGS)
        compose.onNodeWithText("Włącz ją, aby wysłać zdjęcie bariery.").assertIsDisplayed()
        compose.onNodeWithText(SETTINGS).performClick()
        val opened = shadowOf(compose.activity).nextStartedActivity
        assertEquals(
            "otwiera systemowe ustawienia lokalizacji",
            Settings.ACTION_LOCATION_SOURCE_SETTINGS,
            opened.action,
        )

        compose.runOnIdle { locationOn.value = true }
        compose.onNodeWithText("Zrób zdjęcie").assertIsDisplayed()
        compose.onNodeWithText(BACK_TO_MAP).assertDoesNotExist()
    }

    private fun expectNotice(message: String, action: String) {
        compose.onAllNodesWithText("Zgłoś barierę").assertCountEquals(1)
        compose.onNodeWithText("Zmierz i zgłoś barierę").assertDoesNotExist()
        compose.onNodeWithText("Krok 1 z 3").assertDoesNotExist()
        compose.onNodeWithText("Zrób zdjęcie").assertDoesNotExist()
        compose.onNodeWithText(message).assertIsDisplayed()
        compose.onNodeWithText(action).assertExists()
        compose.onNodeWithText(BACK_TO_MAP).assertExists()
    }

    private fun show(vararg grants: String) {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).denyPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        shadowOf(app).grantPermissions(*grants)
        val actions = QuickReportActions(onNext = {}, onSent = {}, onBack = {}, onBackToMap = { mapOpens++ })
        val models =
            QuickReportModels(
                photoTranscoder = PhotoTranscoder(),
                send =
                    quickSendModel(
                        positions = PositionSource { emptyFlow() },
                        saver = ReportSaver { _, _, _ -> },
                        locationSwitch = LocationSwitch { locationOn },
                    ),
            )
        compose.setContent {
            DostepnyKampusTheme {
                CompositionLocalProvider(LocalActivityResultRegistryOwner provides permissionOwner) {
                    QuickReportScreen(route = ReportRoute(), actions = actions, models = models)
                }
            }
        }
    }

    private companion object {
        const val ALLOW = "Pozwól na lokalizację"
        const val SETTINGS = "Otwórz ustawienia"
        const val BACK_TO_MAP = "Wróć do mapy"
    }
}

private class DeniedResultOwner : ActivityResultRegistryOwner {
    override val activityResultRegistry: ActivityResultRegistry =
        object : ActivityResultRegistry() {
            override fun <I, O> onLaunch(
                requestCode: Int,
                contract: ActivityResultContract<I, O>,
                input: I,
                options: ActivityOptionsCompat?,
            ) {
                dispatchResult(requestCode, emptyMap<String, Boolean>())
            }
        }
}
