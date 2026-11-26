/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.review.queue

import io.github.dostepnykampus.core.data.review.ReviewQueue
import io.github.dostepnykampus.core.data.review.Reviews
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.model.review.ReviewClaim
import io.github.dostepnykampus.core.model.review.ReviewDecision
import io.github.dostepnykampus.core.model.review.ReviewOutcome
import io.github.dostepnykampus.core.model.review.SimilarSubmission
import io.github.dostepnykampus.core.model.submission.Submission
import io.github.dostepnykampus.core.model.submission.SubmissionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Stan R1: wczytywanie, kolejka od najdawniej wysłanego, wzięcie, zgłoszenie zajęte i stany wspólne.
 *
 * Wymagania: US10.4, US10.7, BR-11, NFR-50
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReviewQueueViewModelTest {
    @Test
    fun showsOldestFirst() =
        queueTest(queue = ServerReply.Ok(QUEUE.copy(mine = listOf(MINE)))) { model ->
            assertEquals(ReviewQueueUiState.Loading, model.state.value, "przed odpowiedzią serwera")
            runCurrent()
            val expected = ReviewQueueUiState.Ready(waiting = listOf(OLDER, NEWER), mine = listOf(MINE))
            assertEquals(expected, model.state.value, "kolejka od najdawniej wysłanego")
        }

    @Test
    fun claimMovesToMine() =
        queueTest(claim = ServerReply.Ok(ReviewClaim.Claimed(TAKEN))) { model ->
            runCurrent()
            val opened = mutableListOf<String>()
            model.claim(OLDER.id) { opened += it }
            runCurrent()
            val expected = ReviewQueueUiState.Ready(waiting = listOf(NEWER), mine = listOf(TAKEN))
            assertEquals(expected, model.state.value, "wzięte zgłoszenie w „Wziętych przeze mnie”")
            assertEquals(listOf(OLDER.id), opened, "wzięte zgłoszenie otwiera się")
        }

    @Test
    fun takenDisappears() =
        queueTest(claim = ServerReply.Ok(ReviewClaim.Taken)) { model ->
            runCurrent()
            val opened = mutableListOf<String>()
            model.claim(OLDER.id) { opened += it }
            runCurrent()
            val expected = ReviewQueueUiState.Ready(waiting = listOf(NEWER), mine = emptyList(), isTaken = true)
            assertEquals(expected, model.state.value, "zajęte zgłoszenie znika z komunikatem")
            assertEquals(emptyList<String>(), opened, "zajęte zgłoszenie się nie otwiera")
        }

    @Test
    fun showsCommonStates() {
        val cases =
            mapOf(
                ServerReply.Offline to ReviewQueueUiState.Offline,
                ServerReply.Fault(FORBIDDEN) to ReviewQueueUiState.Forbidden,
                ServerReply.Fault(SERVER_ERROR) to ReviewQueueUiState.ServerError,
                ServerReply.Missing to ReviewQueueUiState.ServerError,
            )
        cases.forEach { (reply, expected) ->
            queueTest(queue = reply) { model ->
                runCurrent()
                assertEquals(expected, model.state.value, "stan dla $reply")
            }
        }
    }

    @Test
    fun claimOfflineIsOffline() =
        queueTest(claim = ServerReply.Offline) { model ->
            runCurrent()
            model.claim(OLDER.id) {}
            runCurrent()
            assertEquals(ReviewQueueUiState.Offline, model.state.value, "wzięcie bez internetu")
        }

    @Test
    fun retryReadsAgain() =
        queueTest(queue = ServerReply.Offline) { model ->
            runCurrent()
            model.refresh()
            assertEquals(ReviewQueueUiState.Loading, model.state.value, "wczytywanie po „Spróbuj ponownie”")
            runCurrent()
            assertEquals(ReviewQueueUiState.Offline, model.state.value, "drugi odczyt bez internetu")
        }

    private fun queueTest(
        queue: ServerReply<ReviewQueue> = ServerReply.Ok(QUEUE),
        claim: ServerReply<ReviewClaim> = ServerReply.Missing,
        block: suspend TestScope.(ReviewQueueViewModel) -> Unit,
    ) {
        val reviews = FakeReviews(list = queue, take = claim)
        val dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        try {
            runTest(dispatcher) { block(ReviewQueueViewModel(reviews)) }
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class FakeReviews(private val list: ServerReply<ReviewQueue>, private val take: ServerReply<ReviewClaim>) :
        Reviews {
        override suspend fun queue(): ServerReply<ReviewQueue> = list

        override suspend fun submission(submissionId: String): ServerReply<Submission> = ServerReply.Missing

        override fun photoUrl(submissionId: String, photoId: String): String = photoId

        override suspend fun claim(submissionId: String): ServerReply<ReviewClaim> = take

        override suspend fun decide(submissionId: String, decision: ReviewDecision): ServerReply<ReviewOutcome> =
            ServerReply.Missing

        override suspend fun similar(submissionId: String): ServerReply<List<SimilarSubmission>> = ServerReply.Missing

        override suspend fun photo(submissionId: String, photoId: String): ServerReply<ByteArray> = ServerReply.Missing

        override suspend fun replacePhoto(submissionId: String, photoId: String, jpeg: ByteArray): ServerReply<Unit> =
            ServerReply.Missing
    }

    private companion object {
        const val FORBIDDEN = 403
        const val SERVER_ERROR = 500
        val OLDER = queued(id = "a", day = 26)
        val NEWER = queued(id = "b", day = 27)
        val MINE = queued(id = "c", day = 25).copy(status = SubmissionStatus.IN_REVIEW)
        val TAKEN = OLDER.copy(status = SubmissionStatus.IN_REVIEW)
        val QUEUE = ReviewQueue(waiting = listOf(NEWER, OLDER), mine = emptyList())
    }
}
