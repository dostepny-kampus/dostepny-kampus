/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.accepted

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import io.github.dostepnykampus.app.EXPAND_HANDLE
import io.github.dostepnykampus.app.MainActivity
import io.github.dostepnykampus.app.acceptingQueue
import io.github.dostepnykampus.app.offlineQueue
import io.github.dostepnykampus.app.photoDraft
import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
import io.github.dostepnykampus.core.data.barrier.AcceptedBarriers
import io.github.dostepnykampus.core.data.di.BarrierDataModule
import io.github.dostepnykampus.core.data.map.MapListChoice
import io.github.dostepnykampus.core.data.submission.sync.SubmissionStore
import io.github.dostepnykampus.core.database.submission.PendingSubmissionDao
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.submission.BarrierType
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Przyjęte bariery budynku C-13 z ich źródła w grafie Hilt zostają na karcie budynku po zapisaniu zmiany albo
 * wysłaniu zgłoszenia usunięcia.
 *
 * Wymagania: US8.4, BR-3, BR-18, BR-19
 */
@HiltAndroidTest
@UninstallModules(BarrierDataModule::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, qualifiers = "w360dp-h800dp")
class AcceptedOnMapTest {
    /** Źródło przyjętych barier z pamięci zamiast `BarrierDataModule`; reszta grafu jest prawdziwa. */
    @Module
    @InstallIn(SingletonComponent::class)
    object Accepted {
        @Provides
        @Singleton
        fun barriers(): AcceptedBarriers =
            object : AcceptedBarriers {
                override suspend fun barriers(): List<AcceptedBarrier> = accepted

                override fun syncedAt(): Instant? = syncedAt

                override fun known(): List<AcceptedBarrier>? = accepted
            }
    }

    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var store: SubmissionStore

    @Inject
    lateinit var pending: PendingSubmissionDao

    @Inject
    lateinit var mapListChoice: MapListChoice

    @Before
    fun listAsFirstScreen() {
        hilt.inject()
        runBlocking { mapListChoice.choose(isList = true) }
        compose.waitUntil(WAIT_MS) { compose.onAllNodesWithText("Pomoc").fetchSemanticsNodes().isNotEmpty() }
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    /**
     * Zmiana przyjętego progu w C-13 czeka w kolejce jako zgłoszenie powiązane z przyjętym, a karta C-13 nadal
     * pokazuje ten próg.
     * Scenariusz (częściowo): zgloszenia-zbierajacego.feature: Zmiana zgłoszenia przyjętego tworzy nowe zgłoszenie
     */
    @Test
    fun keepsThresholdAfterChange() {
        val change = content(replaces = "s-0", barrier = THRESHOLD_CODE, severity = 3)
        runBlocking { offlineQueue(store) {}.save(change, photo()) }
        val queued = runBlocking { pending.observe().first() }.single().submission
        assertEquals("zmiana wskazuje przyjęty próg", "s-0", queued.replacesSubmissionId)

        openCard()

        compose.onNodeWithText(THRESHOLD).performScrollTo().assertExists("próg przyjęty na karcie C-13")
    }

    /**
     * Zgłoszenie usunięcia niesprawnej windy wysłane przy dostępnej sieci; karta C-13 nadal pokazuje tę windę,
     * do decyzji Weryfikatora.
     * Scenariusz (częściowo): zgloszenia-zbierajacego.feature: Inny Zbierający niż autor zgłasza usunięcie bariery
     */
    @Test
    fun keepsLiftAfterRemovalSent() {
        runBlocking {
            acceptingQueue(store).apply {
                val removal = content(replaces = "s-9", barrier = LIFT_CODE, severity = null, isRemoved = true)
                save(removal, photo())
                sendAll()
            }
        }
        assertEquals("usunięcie wysłane, kolejka pusta", 0, runBlocking { pending.observe().first() }.size)

        openCard()

        compose.onNodeWithText(LIFT).performScrollTo().assertExists("niesprawna winda na karcie C-13")
    }

    private fun openCard() {
        compose.onNode(hasSetTextAction()).performTextInput("C-13")
        compose.waitUntil(WAIT_MS) { compose.onAllNodes(listedC13).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(listedC13).onFirst().performClick()
        compose.waitUntil(WAIT_MS) { compose.onAllNodesWithText("Zamknij").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription(EXPAND_HANDLE).performClick()
        compose.waitUntil(WAIT_MS) { compose.onAllNodesWithText(BARRIERS_GROUP).fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
    }

    private fun photo() = listOf(photoDraft(RuntimeEnvironment.getApplication().cacheDir, PHOTO_ID))

    private fun content(
        replaces: String,
        barrier: String,
        severity: Int?,
        isRemoved: Boolean? = null,
    ): SubmissionContent =
        SubmissionContent(
            kind = SubmissionKind.PRECISE,
            location = location,
            photoIds = listOf(PHOTO_ID),
            buildingSymbol = "C-13",
            barrierType = barrier,
            severity = severity,
            replacesSubmissionId = replaces,
            isBarrierRemoved = isRemoved,
        )

    private companion object {
        const val WAIT_MS = 5_000L
        const val PHOTO_ID = "p-1"
        const val THRESHOLD = "Próg"
        const val LIFT = "Niesprawna winda"
        const val BARRIERS_GROUP = "Bariery w budynku"
        const val THRESHOLD_CODE = "TB-13"
        const val LIFT_CODE = "TB-9"
        val listedC13: SemanticsMatcher = hasContentDescription("C-13", substring = true)
        val location = GeoPoint(longitude = 17.05, latitude = 51.125)
        val syncedAt: Instant = Instant.parse("2026-11-12T10:00:00Z")
        val accepted =
            listOf(
                acceptedBarrier(id = "s-0", type = BarrierType.THRESHOLD),
                acceptedBarrier(id = "s-9", type = BarrierType.BROKEN_LIFT),
            )

        fun acceptedBarrier(id: String, type: BarrierType): AcceptedBarrier =
            AcceptedBarrier(
                id = id,
                location = location,
                severity = Severity.BLOCKING,
                type = type,
                buildingSymbol = "C-13",
                verifiedAt = syncedAt,
            )
    }
}
