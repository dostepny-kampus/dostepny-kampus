/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.quick

import android.Manifest
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.dostepnykampus.app.AppShell
import io.github.dostepnykampus.app.TestAppScreen
import io.github.dostepnykampus.app.TestRoles
import io.github.dostepnykampus.app.appMapViewModel
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.Account
import io.github.dostepnykampus.feature.capture.navigation.ReportRoute
import io.github.dostepnykampus.feature.capture.photo.PhotoTranscoder
import io.github.dostepnykampus.feature.capture.quick.QuickReportModels
import io.github.dostepnykampus.feature.map.camera.CampusMapView
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class GuestQuickReportTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app = RuntimeEnvironment.getApplication()
    private val roles = TestRoles()
    private val noMap = CampusMapView { _, _, _, _, _, _, _, _, _ -> }

    /**
     * Gość na mapie wybiera „Zgłoś barierę” i dostaje prośbę o logowanie albo założenie konta, a krok ze zdjęciem
     * i aparat się nie otwierają.
     * Scenariusz: zgloszenie-szybkie.feature: Gość niezalogowany jest proszony o logowanie przed zdjęciem
     *
     * Wymagania: US14.2, RO-GOS
     */
    @Test
    fun asksGuestToSignInFirst() {
        showMap()
        compose.onNodeWithText("Zgłoś barierę").performClick()

        compose.onNodeWithText("Aby zgłosić barierę, potrzebne jest konto.").assertExists("prośba o logowanie")
        compose.onNodeWithText("Zaloguj się").assertExists("logowanie")
        compose.onNodeWithText("Załóż konto").assertExists("rejestracja")
        compose.onAllNodesWithText("Krok 1 z 3").assertCountEquals(0)
        compose.onAllNodesWithText("Zrób zdjęcie").assertCountEquals(0)
        assertNull("aparat się nie otwiera", shadowOf(compose.activity).nextStartedActivityForResult)
    }

    /**
     * Gość po prośbie o logowanie loguje się jako Odwiedzający i trafia do kroku 1 zgłoszenia, z którego
     * „Zrób zdjęcie” otwiera aparat.
     * Scenariusz: zgloszenie-szybkie.feature: Gość po zalogowaniu wraca do zgłoszenia szybkiego
     *
     * Wymagania: US14.2, RO-GOS
     */
    @Test
    fun opensCameraAfterSignIn() {
        showMap()
        compose.onNodeWithText("Zgłoś barierę").performClick()
        compose.onNodeWithText("Aby zgłosić barierę, potrzebne jest konto.").assertExists("prośba o logowanie")

        roles.account.value = Account.SignedIn()

        compose.onNodeWithText("Krok 1 z 3").assertExists("krok 1 po zalogowaniu")
        compose.onAllNodesWithText("Aby zgłosić barierę, potrzebne jest konto.").assertCountEquals(0)
        compose.onNodeWithText("Zrób zdjęcie").performClick()
        val camera = shadowOf(compose.activity).nextStartedActivityForResult
        assertEquals("aparat do zdjęcia bariery", MediaStore.ACTION_IMAGE_CAPTURE, camera?.intent?.action)
    }

    private fun showMap() {
        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        val mapModel = compose.activity.appMapViewModel()
        val where = app.quickSendModel(point = INSIDE_D2, networkStatus = { flowOf(true) }, saver = { _, _, _ -> })
        val models = QuickReportModels(photoTranscoder = PhotoTranscoder(), send = where)
        compose.setContent {
            DostepnyKampusTheme {
                AppShell(firstLaunch = false, roles = roles, onFirstHelpLeft = {}) { route ->
                    if (route is ReportRoute) QuickTestScreen(route, models) else TestAppScreen(route, mapModel, noMap)
                }
            }
        }
    }
}
