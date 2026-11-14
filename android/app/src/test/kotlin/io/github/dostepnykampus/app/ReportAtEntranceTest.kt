/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
@file:OptIn(TymczasoweRole::class)

package io.github.dostepnykampus.app

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation3.runtime.NavKey
import io.github.dostepnykampus.app.navigation.AppNavigator
import io.github.dostepnykampus.app.navigation.SignInRoute
import io.github.dostepnykampus.core.data.account.GuestRoleProvider
import io.github.dostepnykampus.core.data.account.RoleProvider
import io.github.dostepnykampus.core.data.account.TymczasoweRole
import io.github.dostepnykampus.core.data.buildings.BUILDINGS_ASSET
import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.model.Account
import io.github.dostepnykampus.feature.capture.navigation.ReportRoute
import io.github.dostepnykampus.feature.map.BuildingCardUiState
import io.github.dostepnykampus.feature.map.buildingCard
import io.github.dostepnykampus.feature.map.navigation.BuildingRoute
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.coroutines.EmptyCoroutineContext

private const val SYMBOL = "C-13"

private val ReportButton = hasClickAction() and hasContentDescription("Zgłoś barierę przy wejściu", substring = true)

/** „Zgłoś barierę przy wejściu” na V2 od dotknięcia do ekranu zgłoszenia albo logowania. Wymagania: US4.6, US14.2 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class ReportAtEntranceTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var navigator: AppNavigator

    /**
     * Wiersz: V2 pełny ekran, zalogowany → „Zgłoś barierę przy wejściu” przy każdym wejściu prowadzi do V4 z
     *   wejściem, przy którym stoi.
     *
     * Wymagania: US4.6
     */
    @Test
    fun us4_6_each_entrance_opens_v4() {
        val ids = entranceIds()
        show(TestRoles(Account.SignedIn()))
        ids.forEachIndexed { index, id ->
            val button = compose.onAllNodes(ReportButton)[index]
            button.performScrollTo()
            button.performClick()
            assertEquals("wejście nr ${index + 1}", ReportRoute(entranceId = id), navigator.current)
            pressBack()
        }
    }

    /**
     * Wiersz: V2 pełny ekran, Gość → ta sama czynność prowadzi najpierw do L1, który pamięta wejście.
     *
     * Wymagania: US14.2
     */
    @Test
    fun us14_2_guest_goes_to_l1() {
        val ids = entranceIds()
        show(GuestRoleProvider)
        ids.forEachIndexed { index, id ->
            val button = compose.onAllNodes(ReportButton)[index]
            button.performScrollTo()
            button.performClick()
            assertEquals(
                "wejście nr ${index + 1}",
                SignInRoute(isForReport = true, entranceId = id),
                navigator.current,
            )
            pressBack()
        }
    }

    private fun entranceIds(): List<String> =
        runBlocking {
            val catalog =
                BuildingCatalogRepository(EmptyCoroutineContext) {
                    val assets = compose.activity.assets
                    assets.open(BUILDINGS_ASSET).bufferedReader().use { it.readText() }
                }
            val card = buildingCard(catalog.catalog(), SYMBOL) as BuildingCardUiState.Ready
            card.entrances.map { it.entrance.id }
        }

    private fun show(roles: RoleProvider) {
        compose.setContent {
            AppShell(firstLaunch = false, roles = roles, onFirstHelpLeft = {}) { route: NavKey ->
                navigator = this
                if (route is MapRoute) {
                    Text(text = "V1", modifier = Modifier.clickable { openBuilding(SYMBOL, isDirect = true) })
                } else if (route is BuildingRoute) {
                    TestBuildingCard(route.symbol)
                }
            }
        }
        compose.onNodeWithText("V1").performClick()
        compose.waitUntil { compose.onAllNodesWithText("Wejścia").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(ReportButton).assertCountEquals(entranceIds().size)
    }

    private fun pressBack() {
        compose.waitForIdle()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }
}
