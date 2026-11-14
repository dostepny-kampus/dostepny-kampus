/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.activity.ComponentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.github.dostepnykampus.core.data.di.DataModule
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.data.submissions.OwnStatuses
import io.github.dostepnykampus.core.data.submissions.OwnSubmissions
import io.github.dostepnykampus.core.data.submissions.PendingPhotoDraft
import io.github.dostepnykampus.core.data.submissions.StatusRefresh
import io.github.dostepnykampus.core.data.submissions.SubmissionQueue
import io.github.dostepnykampus.core.data.submissions.SubmissionServer
import io.github.dostepnykampus.core.data.submissions.SubmissionStamps
import io.github.dostepnykampus.core.data.submissions.SubmissionStore
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.Submission
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.core.model.SubmissionKind
import io.github.dostepnykampus.core.model.SubmissionStatus
import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.feature.capture.mine.MySubmissionsActions
import io.github.dostepnykampus.feature.capture.mine.MySubmissionsContent
import io.github.dostepnykampus.feature.capture.mine.MySubmissionsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Lista „Moje zgłoszenia” na prawdziwej kolejce i historii z grafu Hilt: zapis, przebieg wysyłki z odpowiedzią
 * serwera i karty C3.
 *
 * Wymagania: US8.1, US8.2, BR-9
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, qualifiers = "w360dp-h800dp")
class OwnListPathTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app = RuntimeEnvironment.getApplication()

    @Inject
    lateinit var store: SubmissionStore

    private var isServerReachable = false
    private var today = LocalDate.of(YEAR, OCTOBER, FIRST_DAY)
    private var saved = 0

    @Before
    fun inject() {
        hilt.inject()
    }

    /**
     * Zgłoszenia zapisane 12, 24 i 28 października: na liście pierwsze jest z 28.10, ostatnie z 12.10.
     * Scenariusz: zgloszenia-zbierajacego.feature: Najnowsze zgłoszenia są na górze listy
     */
    @Test
    fun us8_1_newest_first_on_list() {
        val queue = queue()
        listOf(FIRST_DAY, MIDDLE_DAY, LAST_DAY).forEach { day ->
            today = LocalDate.of(YEAR, OCTOBER, day)
            runBlocking { queue.save(precise(), photo()) }
        }

        showMine()

        assertTrue(top("28.10.2026") < top("24.10.2026"))
        assertTrue(top("24.10.2026") < top("12.10.2026"))
    }

    /**
     * Sieć wróciła, ale serwer nie odpowiedział: zgłoszenie zostaje „W telefonie”.
     * Scenariusz (przykład: nie potwierdzi odbioru zgłoszenia): zgloszenia-zbierajacego.feature: Status „wysłane”
     *   dopiero po potwierdzeniu odbioru przez serwer
     */
    @Test
    fun us8_2_unconfirmed_stays_local() {
        sendOne()

        compose.onNodeWithText("W telefonie").assertExists()
        compose.onNodeWithText("Wysłane").assertDoesNotExist()
    }

    /**
     * Serwer potwierdził odbiór: zgłoszenie ma status „Wysłane”.
     * Scenariusz (przykład: potwierdzi odbiór zgłoszenia): zgloszenia-zbierajacego.feature: Status „wysłane”
     *   dopiero po potwierdzeniu odbioru przez serwer
     */
    @Test
    fun us8_2_confirmed_is_sent() {
        isServerReachable = true
        sendOne()

        compose.onNodeWithText("Wysłane").assertExists()
        compose.onNodeWithText("W telefonie").assertDoesNotExist()
    }

    private fun sendOne() {
        val queue = queue()
        runBlocking {
            queue.save(precise(), photo())
            queue.sendAll()
        }
        showMine()
    }

    private fun showMine() {
        val list = runBlocking { DataModule.ownSubmissions(store).observe().first() }
        val viewModel =
            MySubmissionsViewModel(
                history = OwnSubmissions { flowOf(list) },
                statuses = OwnStatuses { StatusRefresh.OFFLINE },
                networkStatus = { flowOf(false) },
            )
        compose.setContent {
            val state by viewModel.state.collectAsState()
            DostepnyKampusTheme {
                MySubmissionsContent(
                    state = state,
                    actions = MySubmissionsActions(onBack = {}, onMeasure = {}, onOpen = {}),
                )
            }
        }
    }

    private fun top(day: String) = compose.onNodeWithText(day, substring = true).getUnclippedBoundsInRoot().top

    private fun queue(): SubmissionQueue =
        SubmissionQueue(
            store = store,
            ioContext = EmptyCoroutineContext,
            server =
                SubmissionServer(
                    create = { id, content -> if (isServerReachable) accepted(id, content) else noAnswer() },
                    putPhoto = { _, _, _ -> ServerReply.Ok(Unit) },
                    update = { _, _, _ -> ApiResult.NotFound },
                ),
            stamps =
                SubmissionStamps(
                    newId = { "s-${saved++}" },
                    nowMillis = { noon(today) },
                ),
        )

    private fun noon(day: LocalDate): Long =
        day
            .atTime(NOON, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    private fun noAnswer(): ServerReply<Submission> = ServerReply.Offline

    private fun accepted(id: String, content: SubmissionContent): ServerReply<Submission> =
        ServerReply.Ok(
            Submission(
                id = id,
                version = 1,
                status = SubmissionStatus.SENT,
                createdAt = Instant.parse("2026-10-12T10:00:00Z"),
                content = content,
            ),
        )

    private fun photo(): List<PendingPhotoDraft> {
        val file = File(app.cacheDir, "p-$saved.jpg")
        file.writeBytes(byteArrayOf(1, 2, 3))
        return listOf(PendingPhotoDraft(photoId = "p-$saved", path = file.path))
    }

    private fun precise(): SubmissionContent =
        SubmissionContent(
            kind = SubmissionKind.PRECISE,
            location = GeoPoint(longitude = 17.06209, latitude = 51.10785),
            photoIds = listOf("p-$saved"),
            buildingSymbol = "A-1",
            barrierType = "TB-1",
            severity = 2,
        )
}

private const val YEAR = 2026
private const val OCTOBER = 10
private const val FIRST_DAY = 12
private const val MIDDLE_DAY = 24
private const val LAST_DAY = 28
private const val NOON = 12
