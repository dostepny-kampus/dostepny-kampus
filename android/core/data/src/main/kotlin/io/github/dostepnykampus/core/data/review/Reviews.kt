/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.review

import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.data.server.toReply
import io.github.dostepnykampus.core.model.review.ReviewClaim
import io.github.dostepnykampus.core.model.review.ReviewDecision
import io.github.dostepnykampus.core.model.review.ReviewOutcome
import io.github.dostepnykampus.core.model.review.SimilarSubmission
import io.github.dostepnykampus.core.model.submission.Submission
import io.github.dostepnykampus.core.model.submission.SubmissionStatus
import io.github.dostepnykampus.core.network.review.ReviewApi

/**
 * Sprawdzanie zgłoszeń przez Weryfikatora na serwerze. Każde wywołanie wymaga sieci; bez niej wynik to
 * [ServerReply.Offline], a nic nie czeka w telefonie. Brak zgłoszenia to [ServerReply.Missing], a 401 i 403
 * to [ServerReply.Fault].
 *
 * Wymagania: US10.1, US10.2, US10.3, US10.4, US10.7, BR-11
 */
public interface Reviews {
    /** Kolejka Weryfikatora podzielona na zgłoszenia do wzięcia i wzięte przez niego. */
    public suspend fun queue(): ServerReply<ReviewQueue>

    /**
     * Bierze zgłoszenie do sprawdzenia.
     *
     * @param submissionId identyfikator zgłoszenia
     */
    public suspend fun claim(submissionId: String): ServerReply<ReviewClaim>

    /**
     * Zapisuje decyzję o zgłoszeniu wziętym przez tego Weryfikatora.
     *
     * @param submissionId identyfikator zgłoszenia
     * @param decision przyjęcie albo odrzucenie z powodem
     */
    public suspend fun decide(submissionId: String, decision: ReviewDecision): ServerReply<ReviewOutcome>

    /**
     * Zgłoszenia do 10 m od sprawdzanego, od najbliższego.
     *
     * @param submissionId identyfikator sprawdzanego zgłoszenia
     */
    public suspend fun similar(submissionId: String): ServerReply<List<SimilarSubmission>>
}

/**
 * Kolejka Weryfikatora od najstarszego zgłoszenia.
 *
 * Wymagania: US10.4, US10.7
 *
 * @property waiting zgłoszenia wysłane, które można wziąć
 * @property mine zgłoszenia w weryfikacji wzięte przez tego Weryfikatora, bez decyzji
 */
public data class ReviewQueue(val waiting: List<Submission>, val mine: List<Submission>)

/**
 * Sprawdzanie zgłoszeń z klienta [api].
 *
 * @param api klient HTTP sprawdzania
 */
internal fun reviewsOf(api: ReviewApi): Reviews =
    object : Reviews {
        override suspend fun queue(): ServerReply<ReviewQueue> =
            api.queue().toReply { all ->
                val (mine, waiting) = all.partition { it.status == SubmissionStatus.IN_REVIEW }
                ReviewQueue(waiting = waiting, mine = mine)
            }

        override suspend fun claim(submissionId: String): ServerReply<ReviewClaim> =
            api.claim(submissionId).toReply { it }

        override suspend fun decide(submissionId: String, decision: ReviewDecision): ServerReply<ReviewOutcome> =
            api.decide(submissionId, decision).toReply { it }

        override suspend fun similar(submissionId: String): ServerReply<List<SimilarSubmission>> =
            api.similar(submissionId).toReply { it }
    }
