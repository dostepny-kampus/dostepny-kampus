/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.github.dostepnykampus.app.navigation.AppNavigator
import io.github.dostepnykampus.app.navigation.AppScreen
import io.github.dostepnykampus.app.navigation.BuildingCard
import io.github.dostepnykampus.app.navigation.reportSaved
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.di.DataModule
import io.github.dostepnykampus.core.data.room.FloorPlans
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.data.submission.own.StatusRefresh
import io.github.dostepnykampus.core.data.submission.sync.SubmissionStore
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.Account
import io.github.dostepnykampus.core.model.StaffRole
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import io.github.dostepnykampus.feature.capture.mine.MySubmissionsActions
import io.github.dostepnykampus.feature.capture.mine.MySubmissionsScreen
import io.github.dostepnykampus.feature.capture.mine.MySubmissionsViewModel
import io.github.dostepnykampus.feature.capture.navigation.MarkingRoute
import io.github.dostepnykampus.feature.capture.navigation.MySubmissionsRoute
import io.github.dostepnykampus.feature.capture.photo.PhotoCamera
import io.github.dostepnykampus.feature.capture.report.ReportSaverModule
import io.github.dostepnykampus.feature.capture.report.marking.MarkingActions
import io.github.dostepnykampus.feature.capture.report.marking.MarkingContent
import io.github.dostepnykampus.feature.capture.report.marking.MarkingStage
import io.github.dostepnykampus.feature.capture.report.marking.MarkingViewModel
import io.github.dostepnykampus.feature.map.building.BuildingCardSources
import io.github.dostepnykampus.feature.map.building.BuildingCardViewModel
import io.github.dostepnykampus.feature.map.navigation.BuildingRoute
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.inject.Inject
import kotlin.coroutines.EmptyCoroutineContext

/** Zmiana oznaczenia od karty budynku przez zapis w telefonie do potwierdzenia przez serwer. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, qualifiers = "w360dp-h800dp")
class EntranceMarkingPathTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Inject
    lateinit var store: SubmissionStore

    private val online = MutableStateFlow(false)
    private val received = mutableListOf<SubmissionContent>()
    private val roles = TestRoles(Account.SignedIn(setOf(StaffRole.COLLECTOR)))
    private val catalog = BuildingCatalogRepository(EmptyCoroutineContext) { CATALOG }
    private val history by lazy { DataModule.ownSubmissions(store, testTokens()) }
    private val queue by lazy {
        queueWith(store) { id, content ->
            if (online.value) {
                received += content
                sentSubmission(id, content)
            } else {
                ServerReply.Offline
            }
        }
    }
    private val marking by lazy {
        MarkingViewModel(catalog, ReportSaverModule.reportSaver(queue), edits = { null })
    }
    private val mine by lazy {
        MySubmissionsViewModel(
            history = history,
            statuses = { if (online.value) StatusRefresh.DONE else StatusRefresh.OFFLINE },
            networkStatus = { online },
        )
    }
    private val card by lazy {
        BuildingCardViewModel(
            symbol = BUILDING,
            repository = catalog,
            networkStatus = { online },
            sources =
                BuildingCardSources(
                    serverEntrances = { emptyList() },
                    acceptedBarriers = { emptyList() },
                    plans = NoFloorPlans,
                    ownSubmissions = history,
                    computeContext = EmptyCoroutineContext,
                ),
        )
    }

    @Before
    fun inject() {
        hilt.inject()
    }

    /**
     * Wysłanie zmiany nie zastępuje oznaczenia na karcie budynku.
     *
     * Scenariusz: oznaczenie-wejscia.feature: Zmiana oznaczenia wejścia przechodzi te same statusy co zgłoszenie
     *
     * Wymagania: US17.2, BR-9
     */
    @Test
    fun sendsAndKeepsEntrance() {
        val photo = photoDraft(compose.activity.cacheDir, PHOTO)
        compose.setContent {
            DostepnyKampusTheme {
                AppShell(firstLaunch = false, roles = roles, onFirstHelpLeft = {}) { route ->
                    TestScreen(route, takePhoto = { marking.takePhoto(photo) })
                }
            }
        }

        waitFor(CHANGE)
        assertUnknownMarking()
        scrollAndClick(CHANGE)
        waitFor("Niedostępne")
        scrollAndClick("Niedostępne")
        scrollAndClick("Zrób zdjęcie")
        compose.onNodeWithText("Wyślij do sprawdzenia").performClick()

        waitFor("W telefonie")
        compose.onNodeWithText("W telefonie").assertIsDisplayed()
        assertTrue("bez sieci serwer nie dostał zgłoszenia", received.isEmpty())
        runBlocking {
            online.value = true
            assertTrue("kolejka wysłała zmianę po powrocie sieci", queue.sendAll())
        }

        waitFor("Wysłane")
        compose.onNodeWithText("Wysłane").assertIsDisplayed()
        compose.onNodeWithText("W telefonie").assertDoesNotExist()
        val submission = received.single()
        assertEquals("zmiana oznaczenia", SubmissionKind.ENTRANCE_MARKING, submission.kind)
        assertEquals("budynek zgłoszenia", BUILDING, submission.buildingSymbol)
        assertEquals("wejście boczne budynku", ENTRANCE, submission.entranceId)
        assertEquals("nowe oznaczenie", EntranceMarking.INACCESSIBLE, submission.entranceMarking)
        assertEquals("zdjęcie wejścia", listOf(PHOTO), submission.photoIds)

        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        waitFor(SIDE_ENTRANCE)
        assertUnknownMarking()
    }

    @Composable
    private fun AppNavigator.TestScreen(route: NavKey, takePhoto: () -> Unit) {
        when (route) {
            is MapRoute -> {
                SideEffect {
                    if (current == route) openBuilding(BUILDING, isDirect = true)
                }
            }

            is BuildingRoute -> {
                val state by card.uiState.collectAsStateWithLifecycle()
                val isOnline by online.collectAsStateWithLifecycle()
                BuildingCard(symbol = route.symbol, state = state, isOnline = isOnline, onRetry = card::retry)
            }

            is MarkingRoute -> {
                EditMarking(route = route, takePhoto = takePhoto, onFinish = { reportSaved() }, onBack = { back() })
            }

            MySubmissionsRoute -> {
                MySubmissionsScreen(
                    actions = MySubmissionsActions(onBack = { back() }, onMeasure = {}, onOpen = {}),
                    viewModel = mine,
                )
            }

            else -> {
                AppScreen(route)
            }
        }
    }

    @Composable
    private fun EditMarking(route: MarkingRoute, takePhoto: () -> Unit, onFinish: () -> Unit, onBack: () -> Unit) {
        val state by marking.uiState.collectAsStateWithLifecycle()
        val finish by rememberUpdatedState(onFinish)
        LaunchedEffect(route) {
            marking.open(route)
            marking.uiState.first { it.stage == MarkingStage.SAVED }
            finish()
        }
        val actions =
            MarkingActions(
                camera = PhotoCamera(takePhoto),
                onChoose = marking::choose,
                onSend = marking::send,
                onBack = onBack,
            )
        MarkingContent(state = state, actions = actions)
    }

    private fun assertUnknownMarking() {
        compose.onNodeWithText(SIDE_ENTRANCE).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Brak danych").assertIsDisplayed()
        compose.onNodeWithText("Niedostępne").assertDoesNotExist()
    }

    private fun waitFor(text: String) {
        compose.waitUntil(timeoutMillis = WAIT_MS) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun scrollAndClick(text: String) {
        compose.onNodeWithText(text).performScrollTo().performClick()
    }

    private object NoFloorPlans : FloorPlans {
        override suspend fun download(symbol: String): Boolean = false

        override suspend fun floors(symbol: String) = null

        override suspend fun rooms(symbol: String, floor: String) = null
    }

    private companion object {
        const val WAIT_MS = 5_000L
        const val BUILDING = "B-8"
        const val ENTRANCE = "node/b-8-side"
        const val SIDE_ENTRANCE = "Wejście boczne"
        const val CHANGE = "Zmień oznaczenie"
        const val PHOTO = "p-wejscie"
        val CATALOG =
            """
            {
              "type": "FeatureCollection",
              "attribution": "© OpenStreetMap",
              "features": [
                {
                  "type": "Feature",
                  "properties": {"kind": "building", "symbols": ["$BUILDING"], "osm_id": "way/715985124"},
                  "geometry": {
                    "type": "Polygon",
                    "coordinates": [[[17.06602,51.10786],[17.06589,51.10790],[17.06581,51.10781],
                                     [17.06595,51.10778],[17.06602,51.10786]]]
                  }
                },
                {
                  "type": "Feature",
                  "properties": {
                    "kind": "entrance", "symbols": ["$BUILDING"], "osm_id": "$ENTRANCE",
                    "entrance": "yes", "ref": "$SIDE_ENTRANCE"
                  },
                  "geometry": {"type": "Point", "coordinates": [17.06587,51.10787]}
                }
              ]
            }
            """.trimIndent()
    }
}
