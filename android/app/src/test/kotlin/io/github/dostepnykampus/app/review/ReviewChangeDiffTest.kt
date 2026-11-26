/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.review

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import coil3.ColorImage
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.intercept.Interceptor
import coil3.request.SuccessResult
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import io.github.dostepnykampus.app.AppShell
import io.github.dostepnykampus.app.HiltTestActivity
import io.github.dostepnykampus.app.HiltTestActivityRule
import io.github.dostepnykampus.app.OfflineDataFakes
import io.github.dostepnykampus.app.TestMapListChoice
import io.github.dostepnykampus.app.TestRoles
import io.github.dostepnykampus.app.navigation.AppScreen
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.building.ServerEntrances
import io.github.dostepnykampus.core.data.di.DataModule
import io.github.dostepnykampus.core.data.di.ReviewDataModule
import io.github.dostepnykampus.core.data.map.MapListChoice
import io.github.dostepnykampus.core.data.review.ReviewQueue
import io.github.dostepnykampus.core.data.review.Reviews
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.data.submission.sync.SubmissionQueue
import io.github.dostepnykampus.core.data.submission.sync.SubmissionStore
import io.github.dostepnykampus.core.data.submission.sync.SubmissionSyncScheduler
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.Account
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.StaffRole
import io.github.dostepnykampus.core.model.building.AssessmentSource
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceAssessment
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.core.model.review.ReviewClaim
import io.github.dostepnykampus.core.model.review.ReviewDecision
import io.github.dostepnykampus.core.model.review.ReviewOutcome
import io.github.dostepnykampus.core.model.review.SimilarSubmission
import io.github.dostepnykampus.core.model.submission.BarrierGroup
import io.github.dostepnykampus.core.model.submission.Submission
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import io.github.dostepnykampus.core.model.submission.SubmissionStatus
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Pełna droga Weryfikatora przez R1 „Zgłoszenia do sprawdzenia” → „Weź do sprawdzenia” → R2 „Sprawdzenie zgłoszenia”
 * w grafie Hilt aplikacji: różnice zmiany od poprzedniego stanu.
 *
 * Wymagania: US10.8, BR-3, BR-19, BR-23, BR-24
 */
@HiltAndroidTest
@UninstallModules(DataModule::class, ReviewDataModule::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, qualifiers = "w360dp-h800dp")
class ReviewChangeDiffTest {
    /** Pamięciowe źródła danych i kolejki Weryfikatora zamiast sieci. */
    @Module(includes = [OfflineDataFakes::class])
    @InstallIn(SingletonComponent::class)
    object Fakes {
        @Provides
        @Singleton
        fun state(): FakeReviewState = FakeReviewState()

        @Provides
        @Singleton
        fun reviews(state: FakeReviewState): Reviews = state

        @Provides
        @Singleton
        fun serverEntrances(state: FakeReviewState): ServerEntrances =
            ServerEntrances { symbol -> state.entrances[symbol].orEmpty() }

        @Provides
        @Singleton
        fun catalog(): BuildingCatalogRepository = BuildingCatalogRepository(EmptyCoroutineContext) { EMPTY_GEOJSON }

        @Provides
        @Singleton
        fun mapChoice(): MapListChoice = TestMapListChoice()

        @Provides
        @Singleton
        fun queue(store: SubmissionStore, sync: SubmissionSyncScheduler): SubmissionQueue =
            SubmissionQueue(
                store = store,
                subject = { "weryfikator" },
                create = { _, _ -> ServerReply.Offline },
                putPhoto = { _, _, _ -> ServerReply.Offline },
                onSaved = sync::schedule,
            )
    }

    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val activity = HiltTestActivityRule()

    @get:Rule(order = 2)
    val compose = createAndroidComposeRule<HiltTestActivity>()

    @Inject
    lateinit var fake: FakeReviewState

    @OptIn(DelicateCoilApi::class)
    @Before
    fun setUp() {
        hilt.inject()
        val loader =
            Interceptor { chain ->
                SuccessResult(image = ColorImage(PHOTO_COLOR, width = 64, height = 48), request = chain.request)
            }
        SingletonImageLoader.setUnsafe(ImageLoader.Builder(compose.activity).components { add(loader) }.build())
    }

    /**
     * Zmiana oznaczenia wejścia głównego A-1 po przyjęciu innej zmiany tego wejścia: napis „Zmienione po wysłaniu”.
     * Scenariusz: weryfikacja.feature: Przy zmianie Weryfikator widzi, czym różni się od poprzedniego stanu.
     */
    @Test
    fun showsChangedEntranceMarking() {
        val assessed =
            EntranceAssessment(date = LocalDate.of(YEAR, OCTOBER, LATER_DAY), source = AssessmentSource.WALKTHROUGH)
        fake.entrances["A-1"] = listOf(MAIN_A1.copy(assessment = assessed))
        val change =
            baseSubmission(
                id = CHANGE_ID,
                content =
                    SubmissionContent(
                        kind = SubmissionKind.ENTRANCE_MARKING,
                        location = POINT_A1,
                        photoIds = listOf("p-wejscie"),
                        buildingSymbol = "A-1",
                        entranceId = MAIN_A1.id,
                        entranceMarking = EntranceMarking.ACCESSIBLE,
                    ),
            )
        fake.enqueue(change)

        claimFirstFromQueue()

        waitForText("Zmienione po wysłaniu")
        compose.onNodeWithText("Zmienione po wysłaniu").performScrollTo().assertIsDisplayed()
    }

    /**
     * Zmiana „bariera usunięta” dla TB-9 w D-2: zdjęcie poprzedniego obok nowego i status „Przyjęte”.
     * Scenariusz: weryfikacja.feature: Przy zmianie Weryfikator widzi, czym różni się od poprzedniego stanu.
     */
    @Test
    fun showsRemovedBesidePrevious() {
        val previous =
            baseSubmission(
                id = PREVIOUS_ID,
                status = SubmissionStatus.ACCEPTED,
                content = liftContent(photoId = "p-poprzednie", isRemoved = false, replaces = null),
            )
        val removal =
            baseSubmission(
                id = CHANGE_ID,
                content = liftContent(photoId = "p-nowe", isRemoved = true, replaces = PREVIOUS_ID),
            )
        fake.stored[PREVIOUS_ID] = previous
        fake.enqueue(removal)

        claimFirstFromQueue()

        waitForText("Zdjęcie z poprzedniego zgłoszenia")
        val oldPhoto = compose.onNodeWithText("Zdjęcie z poprzedniego zgłoszenia").performScrollTo()
        val newPhoto = compose.onNodeWithText("Nowe zdjęcie miejsca").performScrollTo()
        oldPhoto.assertIsDisplayed()
        newPhoto.assertIsDisplayed()
        val oldBounds = oldPhoto.getUnclippedBoundsInRoot()
        val newBounds = newPhoto.getUnclippedBoundsInRoot()
        assertTrue("zdjęcie poprzedniego zgłoszenia obok nowego", oldBounds.right <= newBounds.left)
        compose.onNodeWithText("Poprzednie zgłoszenie:").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Przyjęte").performScrollTo().assertIsDisplayed()
    }

    /**
     * Nowa wersja zgłoszenia TB-13 po zmianie uciążliwości i opisu: nazwy zmienionych pól na R2.
     * Scenariusz: weryfikacja.feature: Przy zmianie Weryfikator widzi, czym różni się od poprzedniego stanu.
     */
    @Test
    fun showsChangedFieldsOfVersion() {
        val initial =
            baseSubmission(
                id = CHANGE_ID,
                content = thresholdContent(severity = 1, description = "Niski próg"),
            )
        val updated =
            initial.copy(
                version = 2,
                content = thresholdContent(severity = 3, description = "Wysoki próg przy wejściu"),
            )
        fake.enqueue(initial)
        fake.newerVersion = updated

        claimFirstFromQueue()
        waitForText("Przyjmij")
        compose.onNodeWithText("Przyjmij").performScrollTo().performClick()

        waitForText("Zmienione: uciążliwość, opis")
        compose.onNodeWithText("Autor zmienił zgłoszenie.").assertIsDisplayed()
        compose.onNodeWithText("Zmienione: uciążliwość, opis").assertIsDisplayed()
    }

    private fun claimFirstFromQueue() {
        val roles = TestRoles(Account.SignedIn(setOf(StaffRole.VERIFIER)))
        compose.setContent {
            DostepnyKampusTheme {
                AppShell(firstLaunch = false, roles = roles, onFirstHelpLeft = {}) { route ->
                    if (route is MapRoute) Text("V1") else AppScreen(route)
                }
            }
        }
        compose.onNodeWithText("praca", substring = true).performClick()
        compose.onNodeWithText("Zgłoszenia do sprawdzenia").performClick()
        waitForText("Weź do sprawdzenia")
        compose.onNodeWithText("Weź do sprawdzenia").performClick()
    }

    private fun waitForText(text: String) {
        compose.waitUntil(WAIT_MS) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Pamięciowa kolejka i zgłoszenia serwera dla testu drogi R1 → R2. */
    class FakeReviewState : Reviews {
        val stored = mutableMapOf<String, Submission>()
        val entrances = mutableMapOf<String, List<Entrance>>()
        private val waiting = mutableListOf<Submission>()
        var newerVersion: Submission? = null

        fun enqueue(submission: Submission) {
            stored[submission.id] = submission
            waiting += submission
        }

        override suspend fun queue(): ServerReply<ReviewQueue> =
            ServerReply.Ok(ReviewQueue(waiting = waiting.toList(), mine = emptyList()))

        override suspend fun submission(submissionId: String): ServerReply<Submission> =
            stored[submissionId]?.let { ServerReply.Ok(it) } ?: ServerReply.Missing

        override fun photoUrl(submissionId: String, photoId: String): String =
            "https://serwer.test/submissions/$submissionId/photos/$photoId"

        override suspend fun claim(submissionId: String): ServerReply<ReviewClaim> {
            val claimed = stored.getValue(submissionId).copy(status = SubmissionStatus.IN_REVIEW)
            stored[submissionId] = claimed
            waiting.removeAll { it.id == submissionId }
            return ServerReply.Ok(ReviewClaim.Claimed(claimed))
        }

        override suspend fun decide(submissionId: String, decision: ReviewDecision): ServerReply<ReviewOutcome> {
            val updated = newerVersion
            return if (updated != null) {
                stored[submissionId] = updated
                ServerReply.Ok(ReviewOutcome.Changed(updated))
            } else {
                val accepted = stored.getValue(submissionId).copy(status = SubmissionStatus.ACCEPTED)
                ServerReply.Ok(ReviewOutcome.Decided(accepted))
            }
        }

        override suspend fun similar(submissionId: String): ServerReply<List<SimilarSubmission>> =
            ServerReply.Ok(emptyList())

        override suspend fun photo(submissionId: String, photoId: String): ServerReply<ByteArray> = ServerReply.Missing

        override suspend fun replacePhoto(submissionId: String, photoId: String, jpeg: ByteArray): ServerReply<Unit> =
            ServerReply.Offline
    }

    private companion object {
        const val WAIT_MS = 5_000L
        const val PHOTO_COLOR = 0xFF336699.toInt()
        const val YEAR = 2026
        const val OCTOBER = 10
        const val SENT_DAY = 20
        const val LATER_DAY = 22
        const val CHANGE_ID = "zmiana-1"
        const val PREVIOUS_ID = "poprzednie-1"
        const val EMPTY_GEOJSON =
            """{"type":"FeatureCollection","attribution":"© OpenStreetMap contributors","features":[]}"""
        val POINT_A1 = GeoPoint(longitude = 17.0604, latitude = 51.1072)
        val POINT_D2 = GeoPoint(longitude = 17.0576, latitude = 51.1098)
        val POINT_C13 = GeoPoint(longitude = 17.0621, latitude = 51.1079)
        val MAIN_A1 =
            Entrance(
                id = "node/1",
                buildingSymbol = "A-1",
                name = "Wejście główne",
                isMain = true,
                type = null,
                location = POINT_A1,
                marking = EntranceMarking.INACCESSIBLE,
            )

        fun baseSubmission(
            id: String,
            content: SubmissionContent,
            status: SubmissionStatus = SubmissionStatus.SENT,
        ): Submission =
            Submission(
                id = id,
                version = 1,
                status = status,
                createdAt = Instant.parse("2026-10-${SENT_DAY}T10:00:00Z"),
                content = content,
            )

        fun liftContent(photoId: String, isRemoved: Boolean, replaces: String?): SubmissionContent =
            SubmissionContent(
                kind = SubmissionKind.PRECISE,
                location = POINT_D2,
                photoIds = listOf(photoId),
                buildingSymbol = "D-2",
                barrierGroup = BarrierGroup.VERTICAL_CIRCULATION,
                barrierType = "TB-9",
                severity = if (isRemoved) null else 3,
                isBarrierRemoved = isRemoved.takeIf { it },
                replacesSubmissionId = replaces,
            )

        fun thresholdContent(severity: Int, description: String): SubmissionContent =
            SubmissionContent(
                kind = SubmissionKind.PRECISE,
                location = POINT_C13,
                photoIds = listOf("p-prog"),
                buildingSymbol = "C-13",
                barrierGroup = BarrierGroup.HORIZONTAL_CIRCULATION,
                barrierType = "TB-13",
                severity = severity,
                description = description,
            )
    }
}
