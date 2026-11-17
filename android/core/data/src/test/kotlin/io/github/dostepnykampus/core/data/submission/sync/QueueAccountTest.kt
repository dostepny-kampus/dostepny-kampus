/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submission.sync

import io.github.dostepnykampus.core.data.account.jwtOf
import io.github.dostepnykampus.core.data.account.sessionOf
import io.github.dostepnykampus.core.data.inspection.FakeMarkerQueue
import io.github.dostepnykampus.core.data.inspection.MarkerSync
import io.github.dostepnykampus.core.data.inspection.floorMarkersOf
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.core.network.auth.AccessTokens
import io.github.dostepnykampus.core.network.inspection.MarkerChangeResult
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/** Wysyłka zapisów offline po zmianie konta. Wymagania: BR-9, BR-26, NFR-33 */
class QueueAccountTest {
    @TempDir
    lateinit var photos: File

    @Test
    fun waitsForSameOwnerAfterLogin() =
        runBlocking {
            val tokens = AccessTokens()
            val session = sessionOf(tokens)
            val sent = mutableListOf<String>()
            val submissions =
                SubmissionQueue(
                    store = FakeStore(),
                    server =
                        SubmissionServer(
                            subject = tokens::subject,
                            create = { owner, id, content ->
                                sent += owner
                                ServerReply.Ok(SubmissionFixtures.server(id, content))
                            },
                            putPhoto = { _, _, _, _ -> ServerReply.Ok(Unit) },
                            update = { _, _, _, _ -> ServerReply.Missing },
                        ),
                )
            val pending = FakeMarkerQueue()
            val markers = floorMarkersOf(queue = pending, subjects = tokens.subjects)
            val sync =
                MarkerSync(pending, tokens::subject) { owner, changes ->
                    sent += owner
                    ApiResult.Success(changes.map { MarkerChangeResult(it.operationId, isApplied = true) })
                }
            session.signIn(jwtOf("""{"sub":"a"}"""))
            submissions.save(SubmissionFixtures.precise("p"), SubmissionFixtures.photo(photos, "p"))
            markers.mark("A-1", "p2")
            session.signOut()
            session.signIn(jwtOf("""{"sub":"b"}"""))
            submissions.onLoggedIn()
            submissions.sendAll()
            sync.sendAll()
            assertEquals(emptyList<String>(), sent, "drugie konto nie wysyła oczekujących zapisów")

            session.signOut()
            session.signIn(jwtOf("""{"sub":"a"}"""))
            submissions.onLoggedIn()
            submissions.sendAll()
            sync.sendAll()
            assertEquals(listOf("a", "a"), sent, "właściciel wznawia wysyłkę obu kolejek")
        }
}
