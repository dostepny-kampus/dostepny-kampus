/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.quick

import android.Manifest
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavKey
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.github.dostepnykampus.app.TestRoles
import io.github.dostepnykampus.app.acceptingQueue
import io.github.dostepnykampus.app.navigation.AppNavigator
import io.github.dostepnykampus.app.photoDraft
import io.github.dostepnykampus.core.data.submission.sync.SubmissionStore
import io.github.dostepnykampus.core.database.submission.PendingSubmissionDao
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.Account
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.submission.BarrierGroup
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import io.github.dostepnykampus.feature.capture.navigation.ReportRoute
import io.github.dostepnykampus.feature.capture.photo.PhotoTranscoder
import io.github.dostepnykampus.feature.capture.quick.QuickLookup
import io.github.dostepnykampus.feature.capture.quick.QuickReportModels
import io.github.dostepnykampus.feature.capture.report.ReportSaverModule
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, qualifiers = "w360dp-h800dp")
class QuickThreeStepsTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app = RuntimeEnvironment.getApplication()

    @Inject
    lateinit var store: SubmissionStore

    @Inject
    lateinit var pending: PendingSubmissionDao

    private val stack = mutableStateListOf<NavKey>(MapRoute())
    private val navigator = AppNavigator(backStack = stack, roles = TestRoles(Account.SignedIn()))

    @Before
    fun inject() {
        hilt.inject()
    }

    /**
     * Zalogowany Odwiedzający w D-2 przechodzi zdjęcie, grupę z piętrem i „Wyślij”, w krokach 1, 2 i 3,
     * a zgłoszenie czeka w kolejce z pozycją telefonu, o którą ekrany nie pytały.
     * Scenariusz: zgloszenie-szybkie.feature: Zgłoszenie szybkie w 3 krokach z pozycją dołączoną automatycznie
     *
     * Wymagania: US4.1
     */
    @Test
    fun sendsInThreeStepsWithPosition() {
        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        val saver = ReportSaverModule.reportSaver(acceptingQueue(store))
        val where = app.quickSendModel(point = INSIDE_D2, networkStatus = { flowOf(true) }, saver = saver)
        val models = QuickReportModels(photoTranscoder = PhotoTranscoder(), send = where)
        compose.setContent {
            DostepnyKampusTheme { (stack.last() as? ReportRoute)?.let { navigator.QuickTestScreen(it, models) } }
        }

        navigator.report()
        assertTrue("zalogowany wchodzi w zgłoszenie, nie w logowanie", stack.last() is ReportRoute)
        compose.onNodeWithText("Krok 1 z 3").assertExists("krok 1: zdjęcie")
        waitFor { where.lookup.value is QuickLookup.Found }
        val position = (where.lookup.value as QuickLookup.Found).position
        assertEquals("telefon stoi w budynku D-2", "D-2", position.buildingSymbol)
        val photo = photoDraft(app.cacheDir, "zdjecie-windy")
        navigator.nextReportStep(ReportRoute(photoId = photo.photoId, photoPath = photo.path))
        compose.onNodeWithText("Krok 2 z 3").assertExists("krok 2: grupa barier po zdjęciu")
        compose.onNodeWithText("Schody i windy").performClick()
        compose.onNodeWithText("Na którym piętrze?").assertExists("pytanie o piętro w budynku")
        compose.onNodeWithText("1").performClick()
        compose.onNodeWithText("Krok 3 z 3").assertExists("krok 3: wysłanie")
        compose.onNodeWithText("Dołączymy miejsce, w którym teraz stoisz.").assertExists("pozycja dołączana sama")
        compose.onNodeWithText("Wyślij").performClick()
        waitFor { compose.onAllNodesWithText("Dziękujemy", substring = true).fetchSemanticsNodes().isNotEmpty() }

        val stored = runBlocking { pending.all() }.single().submission
        assertEquals("zgłoszenie szybkie", SubmissionKind.QUICK.name, stored.kind)
        assertEquals("grupa z kroku 2", BarrierGroup.VERTICAL_CIRCULATION.name, stored.barrierGroup)
        assertEquals("piętro z kroku 2", 1, stored.floorNumber)
        assertEquals("pozycja telefonu", INSIDE_D2, GeoPoint(longitude = stored.longitude, latitude = stored.latitude))
        assertEquals("dokładność pozycji telefonu", FIX_ACCURACY, stored.accuracyMeters)
    }

    private fun waitFor(condition: () -> Boolean) {
        compose.waitUntil(WAIT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            condition()
        }
    }
}

private const val WAIT_MS = 10_000L
