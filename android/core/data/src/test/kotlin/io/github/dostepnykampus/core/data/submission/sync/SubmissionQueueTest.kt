/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submission.sync

import io.github.dostepnykampus.core.data.CapturedLogs
import io.github.dostepnykampus.core.data.account.jwtOf
import io.github.dostepnykampus.core.data.account.sessionOf
import io.github.dostepnykampus.core.data.inspection.FakeMarkerQueue
import io.github.dostepnykampus.core.data.inspection.MarkerSync
import io.github.dostepnykampus.core.data.inspection.floorMarkersOf
import io.github.dostepnykampus.core.data.server.ServerEdit
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.submission.Submission
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import io.github.dostepnykampus.core.model.submission.SubmissionStatus
import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.core.network.auth.AccessTokens
import io.github.dostepnykampus.core.network.inspection.MarkerChangeResult
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.Instant
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Kolejka wysyła treść raz i tylko brakujące zdjęcia, a błędy zostawiają zgłoszenie w kolejce.
 *
 * Wymagania: NFR-5, US9.2
 */
class SubmissionQueueTest {
    @TempDir
    lateinit var tempDir: File

    private val store = FakeStore()
    private val edits = mutableListOf<Pair<String, Int>>()
    private var creates = 0
    private val editResults = ArrayDeque<ServerReply<ServerEdit>>()

    @Test
    fun sendsAndClearsQueue() =
        runBlocking {
            saveTwoPhotos()
            assertTrue(queue().sendAll())

            assertEquals(listOf(SENT_ID to "p1", SENT_ID to "p2"), store.photos)
            assertTrue(store.rows.isEmpty())
        }

    @Test
    fun logsSummaryOfSendPass() =
        runBlocking {
            saveTwoPhotos()
            val lines = CapturedLogs.capture { runBlocking { queue().sendAll() } }

            assertEquals(
                listOf("Przebieg wysyłki zgłoszeń: {SENT=1}, zatrzymanie: null"),
                lines,
                "linia logu z wynikami przebiegu",
            )
        }

    @Test
    fun resumesPhotosAfterDrop() =
        runBlocking {
            saveTwoPhotos()
            store.photoResults.addLast(ServerReply.Offline)
            assertFalse(queue().sendAll())

            assertEquals(SENT_ID, store.created.single())
            val row = store.rows.single()
            assertTrue(store.photos.isEmpty() && row.photos.none { it.isUploaded })

            assertTrue(queue().sendAll())

            assertEquals(1, store.created.size)
            assertEquals(listOf(SENT_ID to "p1", SENT_ID to "p2"), store.photos)
            assertTrue(store.rows.isEmpty())
        }

    @Test
    fun keepsRefusedInQueue() =
        runBlocking {
            saveTwoPhotos()
            store.createResults.addLast(ServerReply.Fault(code = 400))
            queue().sendAll()

            assertTrue(store.created.isEmpty())
            assertTrue(store.photos.isEmpty())
            val left = store.rows.single()
            assertTrue(left.submission.isCreatedOnServer == false)
            assertTrue(left.submission.isRefused)
        }

    /**
     * Odmowa serwera (4xx) nie wraca do wysyłki przy kolejnych próbach; ponowna tylko z decyzji osoby.
     *
     * Wymagania: BR-26
     */
    @Test
    fun skipsRetryOfRejected() =
        runBlocking {
            saveTwoPhotos()
            store.createResults.addLast(ServerReply.Fault(code = 400))

            queue().sendAll()
            assertTrue(queue().sendAll())
            assertTrue(queue().sendAll())

            assertEquals(1, creates)
        }

    @Test
    fun schedulesSendOnSave() =
        runBlocking {
            var scheduled = 0
            saveTwoPhotos(queue(onSaved = { scheduled++ }))

            assertEquals(1, scheduled)
        }

    @Test
    fun replacesRowOnFix() =
        runBlocking {
            val target = queue()
            saveTwoPhotos(target)

            val edited = EditedSubmission(id = SENT_ID, baseVersion = null)
            target.saveEdit(edited, content = fullContent().copy(severity = 3), photos = drafts())

            assertEquals(listOf(SENT_ID), store.rows.map { it.submission.id })
            assertEquals(listOf(3), store.rows.map { it.submission.severity })
        }

    @Test
    fun sendsVersionedFixAsEdit() =
        runBlocking {
            val target = queue()
            target.saveEdit(EditedSubmission(id = "s1", baseVersion = 4), fullContent(), drafts())

            assertTrue(target.sendAll())

            assertEquals(listOf("s1" to 4), edits)
            assertEquals(0, creates)
            assertEquals(emptyList<String>(), store.rows.map { it.submission.id })
        }

    @Test
    fun keepsVersionConflict() =
        runBlocking {
            val target = queue()
            target.saveEdit(EditedSubmission(id = "s1", baseVersion = 4), fullContent(), drafts())
            val server = (serverSubmission(fullContent()) as ServerReply.Ok).value
            editResults.addLast(ServerReply.Ok(ServerEdit.Conflict(server)))

            target.sendAll()

            assertEquals(listOf("s1"), store.rows.map { it.submission.id })
            assertEquals(emptyList<Pair<String, String>>(), store.photos)
        }

    /**
     * Dwa zapisy w tej samej chwili zegara idą na serwer w kolejności zapisu, nie identyfikatorów.
     *
     * Wymagania: US9.2, BR-9
     */
    @Test
    fun keepsSaveOrder() =
        runBlocking {
            val ids = ArrayDeque(listOf("z-pierwsze", "a-drugie"))
            val sent = mutableListOf<String>()
            val target =
                SubmissionQueue(
                    store = store,
                    ioContext = EmptyCoroutineContext,
                    server =
                        submissionServer(
                            create = { id, content ->
                                sent.add(id)
                                serverSubmission(content)
                            },
                            putPhoto = { _, _, _ -> ServerReply.Ok(Unit) },
                            update = { _, _, _ -> ServerReply.Missing },
                        ),
                    stamps = SubmissionStamps(newId = { ids.removeFirst() }, nowMillis = { SAME_MILLIS }),
                )
            target.save(fullContent(), drafts())
            target.save(fullContent(), drafts())
            target.sendAll()

            assertEquals(listOf("z-pierwsze", "a-drugie"), sent)
        }

    /**
     * Wpisy zgłoszenia i znacznika czekają na konto właściciela.
     *
     * Scenariusz: synchronizacja.feature: Kolejki czekają na zalogowanie właściciela
     */
    @Test
    fun waitsForSameOwnerAfterLogin() =
        runBlocking {
            val tokens = AccessTokens()
            var scheduled = 0
            val submissions = queue(subject = tokens::subject, onSaved = { scheduled++ })
            val session = sessionOf(tokens, onLogin = submissions::onLoggedIn)
            val markerOwners = mutableListOf<String>()
            val pending = FakeMarkerQueue()
            val markers = floorMarkersOf(queue = pending, subjects = tokens.subjects)
            val sync =
                MarkerSync(pending, tokens::subject) { owner, changes ->
                    markerOwners += owner
                    ApiResult.Success(changes.map { MarkerChangeResult(it.operationId, isApplied = true) })
                }

            session.signIn(jwtOf("""{"sub":"a","realm_access":{"roles":["COLLECTOR"]}}"""))
            submissions.save(fullContent(), drafts())
            markers.mark("A-1", "p2")
            store.createResults.add(ServerReply.Fault(401))
            submissions.sendAll()
            assertTrue(submissions.loginExpired.value, "odmowa serwera wstrzymuje wysyłkę")

            session.signIn(jwtOf("""{"sub":"b","realm_access":{"roles":["COLLECTOR"]}}"""))
            submissions.sendAll()
            sync.sendAll()
            assertEquals(1, creates, "drugie konto nie próbuje wysłać zgłoszenia")
            assertTrue(markerOwners.isEmpty(), "drugie konto nie próbuje wysłać znacznika")

            session.signIn(jwtOf("""{"sub":"a","realm_access":{"roles":["COLLECTOR"]}}"""))
            submissions.sendAll()
            sync.sendAll()
            assertEquals(listOf(SENT_ID), store.created, "zgłoszenie wysłane po powrocie właściciela")
            assertEquals(listOf("a"), markerOwners, "znacznik wysłany przez właściciela")
            assertEquals(4, scheduled, "zapis i każde logowanie planują wysyłkę")
        }

    private fun queue(subject: () -> String? = { "konto-testowe" }, onSaved: () -> Unit = {}): SubmissionQueue =
        SubmissionQueue(
            store = store,
            ioContext = EmptyCoroutineContext,
            server =
                submissionServer(
                    subject = subject,
                    create = { _, content ->
                        creates++
                        store.createResults.removeFirstOrNull() ?: serverSubmission(content)
                    },
                    putPhoto = { _, _, _ -> store.photoResults.removeFirstOrNull() ?: ServerReply.Ok(Unit) },
                    update = { id, version, content ->
                        edits.add(id to version)
                        editResults.removeFirstOrNull() ?: ServerReply.Ok(ServerEdit.Saved(edited(id, content)))
                    },
                ),
            stamps = SubmissionStamps(newId = { SENT_ID }),
            onSaved = onSaved,
        )

    private fun edited(id: String, content: SubmissionContent): Submission =
        (serverSubmission(content) as ServerReply.Ok).value.copy(id = id, version = 5)

    private fun saveTwoPhotos(target: SubmissionQueue = queue()) {
        runBlocking { target.save(fullContent(), drafts()) }
    }

    private fun drafts(): List<PendingPhotoDraft> {
        val drafts = listOf("p1", "p2").map { photo -> PendingPhotoDraft(photoId = photo, path = pathOf(photo)) }
        drafts.forEach { File(it.path).writeBytes(byteArrayOf(1, 2, 3)) }
        return drafts
    }

    private fun pathOf(photo: String): String = tempDir.resolve("$photo.jpg").path

    private fun serverSubmission(content: SubmissionContent): ServerReply<Submission> =
        ServerReply.Ok(
            Submission(
                id = "s1",
                version = 1,
                status = SubmissionStatus.SENT,
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                content = content,
            ),
        )

    private fun fullContent(): SubmissionContent =
        SubmissionContent(
            kind = SubmissionKind.QUICK,
            location = GeoPoint(longitude = 17.06209, latitude = 51.10785),
            photoIds = listOf("p1", "p2"),
            severity = 2,
            description = "Progi przy sekretariacie",
        )

    private companion object {
        const val SAME_MILLIS = 1_000L
    }
}
