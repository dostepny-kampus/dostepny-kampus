/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.review.check

import io.github.dostepnykampus.core.data.review.ReviewQueue
import io.github.dostepnykampus.core.data.review.Reviews
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.model.review.ReviewClaim
import io.github.dostepnykampus.core.model.review.ReviewDecision
import io.github.dostepnykampus.core.model.review.ReviewOutcome
import io.github.dostepnykampus.core.model.review.SimilarSubmission
import io.github.dostepnykampus.core.model.submission.Submission
import io.github.dostepnykampus.feature.review.queue.queued
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
 * Stan R2: zgłoszenie z podobnymi, przyjęcie, odrzucenie z powodem, nowa wersja, brak sieci i stany wspólne.
 *
 * Wymagania: US10.1, US10.2, US10.8, BR-3, BR-13, BR-16, NFR-50
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReviewCheckViewModelTest {
    private val decisions = mutableListOf<ReviewDecision>()
    private var done = 0

    @Test
    fun showsSubmissionAndSimilar() =
        checkTest { model ->
            assertEquals(CheckUiState.Loading, model.state.value, "przed odpowiedzią serwera")
            runCurrent()
            val photos = listOf("${CHECKED.id}/p-${CHECKED.id}")
            val expected =
                CheckUiState.Ready(
                    submission = CHECKED,
                    photoUrls = photos,
                    similar = listOf(NEAR),
                    similarPhotos = mapOf(NEAR.submission.id to "b/p-b"),
                )
            assertEquals(expected, model.state.value, "zgłoszenie z adresami zdjęć i podobnymi")
        }

    @Test
    fun showsCommonStates() {
        val cases =
            listOf(
                Fake(found = ServerReply.Offline) to CheckUiState.Offline,
                Fake(found = ServerReply.Fault(FORBIDDEN)) to CheckUiState.Forbidden,
                Fake(similar = ServerReply.Fault(SERVER_ERROR)) to CheckUiState.ServerError,
            )
        cases.forEach { (fake, expected) ->
            checkTest(fake) { model ->
                runCurrent()
                assertEquals(expected, model.state.value, "stan dla $fake")
            }
        }
    }

    @Test
    fun acceptReturnsToQueue() =
        checkTest { model ->
            runCurrent()
            model.accept { done++ }
            runCurrent()
            assertEquals(listOf(ReviewDecision.Accept(version = 1)), decisions, "przyjęcie ocenionej wersji")
            assertEquals(1, done, "powrót do R1 po decyzji")
        }

    @Test
    fun rejectNeedsReason() =
        checkTest { model ->
            runCurrent()
            model.reject(reason = "  ", duplicateOf = null) { done++ }
            runCurrent()
            assertEquals(CheckNotice.REASON_MISSING, (model.state.value as CheckUiState.Ready).notice, "komunikat")
            assertEquals(emptyList<ReviewDecision>(), decisions, "bez powodu nic nie idzie na serwer")
        }

    @Test
    fun rejectsDuplicate() =
        checkTest { model ->
            runCurrent()
            model.reject(reason = "Duplikat", duplicateOf = NEAR.submission.id) { done++ }
            model.reject(reason = " Nieaktualne ", duplicateOf = null) { done++ }
            runCurrent()
            val expected =
                listOf(
                    ReviewDecision.RejectDuplicate(version = 1, reason = "Duplikat", duplicateOf = NEAR.submission.id),
                    ReviewDecision.Reject(version = 1, reason = "Nieaktualne"),
                )
            assertEquals(expected, decisions, "odrzucenie duplikatu i odrzucenie z powodem")
        }

    @Test
    fun changedShowsNewVersion() {
        val server = CHECKED.copy(version = 2)
        checkTest(Fake(outcome = ServerReply.Ok(ReviewOutcome.Changed(server)))) { model ->
            runCurrent()
            model.accept { done++ }
            runCurrent()
            val shown = model.state.value as CheckUiState.Ready
            assertEquals(server to CheckNotice.CHANGED, shown.submission to shown.notice, "nowa wersja z komunikatem")
            assertEquals(0, done, "bez powrotu do R1")
        }
    }

    @Test
    fun offlineDecisionWaits() =
        checkTest(Fake(outcome = ServerReply.Offline)) { model ->
            runCurrent()
            model.accept { done++ }
            runCurrent()
            assertEquals(CheckNotice.OFFLINE, (model.state.value as CheckUiState.Ready).notice, "decyzja bez sieci")
        }

    private fun checkTest(fake: Fake = Fake(), block: suspend TestScope.(ReviewCheckViewModel) -> Unit) {
        val dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        try {
            runTest(dispatcher) { block(ReviewCheckViewModel(CHECKED.id, fake)) }
        } finally {
            Dispatchers.resetMain()
        }
    }

    private inner class Fake(
        private val found: ServerReply<Submission> = ServerReply.Ok(CHECKED),
        private val similar: ServerReply<List<SimilarSubmission>> = ServerReply.Ok(listOf(NEAR)),
        private val outcome: ServerReply<ReviewOutcome> = ServerReply.Ok(ReviewOutcome.Decided(CHECKED)),
    ) : Reviews {
        override suspend fun queue(): ServerReply<ReviewQueue> = ServerReply.Missing

        override suspend fun submission(submissionId: String): ServerReply<Submission> = found

        override fun photoUrl(submissionId: String, photoId: String): String = "$submissionId/$photoId"

        override suspend fun claim(submissionId: String): ServerReply<ReviewClaim> = ServerReply.Missing

        override suspend fun decide(submissionId: String, decision: ReviewDecision): ServerReply<ReviewOutcome> {
            decisions += decision
            return outcome
        }

        override suspend fun similar(submissionId: String): ServerReply<List<SimilarSubmission>> = similar

        override fun toString(): String = "$found, $similar"
    }

    private companion object {
        const val FORBIDDEN = 403
        const val SERVER_ERROR = 500
        val CHECKED = queued(id = "a", day = 27)
        val NEAR = SimilarSubmission(submission = queued(id = "b", day = 20), distanceMeters = 4.0)
    }
}
