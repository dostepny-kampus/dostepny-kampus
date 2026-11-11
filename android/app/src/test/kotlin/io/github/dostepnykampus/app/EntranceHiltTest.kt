/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import android.app.Application
import android.view.accessibility.AccessibilityManager
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.buildings.ServerEntrances
import io.github.dostepnykampus.core.data.di.DataModule
import io.github.dostepnykampus.core.data.map.MapListChoice
import io.github.dostepnykampus.core.data.network.NetworkStatus
import io.github.dostepnykampus.core.data.start.FirstLaunch
import io.github.dostepnykampus.core.data.submissions.OwnStatuses
import io.github.dostepnykampus.core.data.submissions.OwnSubmissions
import io.github.dostepnykampus.core.data.submissions.PendingEdits
import io.github.dostepnykampus.core.data.submissions.StatusRefresh
import io.github.dostepnykampus.core.data.submissions.SubmissionQueue
import io.github.dostepnykampus.core.data.submissions.SubmissionSyncScheduler
import io.github.dostepnykampus.core.database.PendingSubmissionDao
import io.github.dostepnykampus.core.network.SubmissionsApi
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import javax.inject.Singleton
import kotlin.coroutines.EmptyCoroutineContext

private const val CATALOG =
    """{"type":"FeatureCollection","attribution":"© OpenStreetMap contributors","features":[""" +
        """{"type":"Feature","properties":{"kind":"building","symbols":["A-1"],"name":"Budynek testowy",""" +
        """"osm_id":"way/1"},"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,1],[0,0]]]}},""" +
        """{"type":"Feature","properties":{"kind":"entrance","symbols":["A-1"],"osm_id":"node/10",""" +
        """"entrance":"yes","ref":"Wejście dostosowane","wheelchair":"yes"},""" +
        """"geometry":{"type":"Point","coordinates":[0.2,0.3]}}]}"""

/**
 * „Pokaż wejście na mapie” przez prawdziwe `AppScreen` i graf Hilt: V2, V3, V1 z nazwą zaznaczonego wejścia dla
 * TalkBack (US3.3, NFR-18, decyzje 134, 135). Dane są w pamięci, bo katalog w zasobach nie ma wejść dostępnych.
 */
@HiltAndroidTest
@UninstallModules(DataModule::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, qualifiers = "w360dp-h800dp")
class EntranceHiltTest {
    /** Źródła danych z pamięci zamiast `DataModule`; katalog „Budynek testowy” nie jest danymi PWr. */
    @Module
    @InstallIn(SingletonComponent::class)
    object Fakes {
        @Provides
        @Singleton
        fun catalog(): BuildingCatalogRepository = BuildingCatalogRepository(EmptyCoroutineContext) { CATALOG }

        @Provides
        @Singleton
        fun serverEntrances(): ServerEntrances = ServerEntrances { emptyList() }

        @Provides
        @Singleton
        fun mapChoice(): MapListChoice = TestMapListChoice().apply { savedChoice.value = true }

        @Provides
        @Singleton
        fun network(): NetworkStatus = NetworkStatus { flowOf(true) }

        @Provides
        @Singleton
        fun firstLaunch(): FirstLaunch =
            object : FirstLaunch {
                override suspend fun isFirstLaunch(): Boolean = false

                override suspend fun markHelpSeen() = Unit
            }

        @Provides
        fun ownSubmissions(): OwnSubmissions = OwnSubmissions { flowOf(emptyList()) }

        @Provides
        fun pendingEdits(): PendingEdits = PendingEdits { null }

        @Provides
        fun ownStatuses(): OwnStatuses = OwnStatuses { StatusRefresh.OFFLINE }

        @Provides
        @Singleton
        fun sync(app: Application): SubmissionSyncScheduler = SubmissionSyncScheduler(app)

        @Provides
        @Singleton
        fun queue(api: SubmissionsApi, store: PendingSubmissionDao, sync: SubmissionSyncScheduler): SubmissionQueue =
            SubmissionQueue(store = store, create = api::create, putPhoto = api::putPhoto, onSaved = sync::schedule)
    }

    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun talkBackOn() {
        hilt.inject()
        shadowOf(compose.activity.getSystemService(AccessibilityManager::class.java)).setTouchExplorationEnabled(true)
    }

    private fun waitFor(matcher: SemanticsMatcher) {
        compose.waitUntil(timeoutMillis = 5_000) { compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
    }

    /** Wiersz: V1 lista, budynek → V2 → V3 → „Pokaż wejście na mapie” → V1, TalkBack czyta wejście i budynek. */
    @Test
    fun us3_3_hilt_entrance_selected() {
        waitFor(ListedA1)
        compose.onNode(ListedA1).performClick()
        waitFor(hasText("Sprawdź, którędy wejść"))
        compose.onNodeWithText("Sprawdź, którędy wejść").performClick()
        waitFor(hasText("Pokaż wejście na mapie"))
        compose.onAllNodes(hasText("Pokaż wejście na mapie")).onFirst().performClick()
        waitFor(hasContentDescription("Zaznaczone wejście: »Wejście dostosowane«, budynek A-1"))
    }
}
