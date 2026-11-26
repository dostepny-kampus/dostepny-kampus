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
import io.github.dostepnykampus.core.network.submission.SubmissionsApi

/**
 * Sprawdzanie zgłoszeń przez Weryfikatora na serwerze. Każde wywołanie wymaga sieci; bez niej wynik to
 * [ServerReply.Offline], a nic nie czeka w telefonie. Brak zgłoszenia to [ServerReply.Missing], a 401 i 403
 * to [ServerReply.Fault].
 *
 * Wymagania: US10.1, US10.2, US10.3, US10.4, US10.7, BR-11, BR-15
 */
public interface Reviews {
    /** Kolejka Weryfikatora podzielona na zgłoszenia do wzięcia i wzięte przez niego. */
    public suspend fun queue(): ServerReply<ReviewQueue>

    /**
     * Zgłoszenie do sprawdzenia w bieżącej wersji.
     *
     * @param submissionId identyfikator zgłoszenia
     */
    public suspend fun submission(submissionId: String): ServerReply<Submission>

    /**
     * Adres zdjęcia zgłoszenia dla biblioteki obrazów, która pobiera je z tokenem konta.
     *
     * @param submissionId identyfikator zgłoszenia
     * @param photoId identyfikator zdjęcia z listy `photoIds`
     */
    public fun photoUrl(submissionId: String, photoId: String): String

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

    /**
     * Zdjęcie zgłoszenia jako JPEG do zasłonięcia twarzy i tablic.
     *
     * Wymagania: BR-15, NFR-25
     *
     * @param submissionId identyfikator zgłoszenia
     * @param photoId identyfikator zdjęcia z listy `photoIds`
     */
    public suspend fun photo(submissionId: String, photoId: String): ServerReply<ByteArray>

    /**
     * Zastępuje zdjęcie zgłoszenia wziętego przez tego Weryfikatora wersją z zasłoniętymi twarzami i tablicami;
     * nie tworzy nowej wersji zgłoszenia.
     *
     * Wymagania: BR-15, NFR-25
     *
     * @param submissionId identyfikator zgłoszenia
     * @param photoId identyfikator zdjęcia z listy `photoIds`
     * @param jpeg plik JPEG z zasłonami w pikselach, już obrócony
     */
    public suspend fun replacePhoto(submissionId: String, photoId: String, jpeg: ByteArray): ServerReply<Unit>
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
 * @param submissions klient HTTP odczytu zgłoszenia i adresu jego zdjęcia
 */
internal fun reviewsOf(api: ReviewApi, submissions: SubmissionsApi): Reviews =
    object : Reviews {
        override suspend fun queue(): ServerReply<ReviewQueue> =
            api.queue().toReply { all ->
                val (mine, waiting) = all.partition { it.status == SubmissionStatus.IN_REVIEW }
                ReviewQueue(waiting = waiting, mine = mine)
            }

        override suspend fun submission(submissionId: String): ServerReply<Submission> =
            submissions.submission(submissionId).toReply { it }

        override fun photoUrl(submissionId: String, photoId: String): String =
            submissions.photoUrl(submissionId = submissionId, photoId = photoId)

        override suspend fun claim(submissionId: String): ServerReply<ReviewClaim> =
            api.claim(submissionId).toReply { it }

        override suspend fun decide(submissionId: String, decision: ReviewDecision): ServerReply<ReviewOutcome> =
            api.decide(submissionId, decision).toReply { it }

        override suspend fun similar(submissionId: String): ServerReply<List<SimilarSubmission>> =
            api.similar(submissionId).toReply { it }

        override suspend fun photo(submissionId: String, photoId: String): ServerReply<ByteArray> =
            submissions.photo(submissionId = submissionId, photoId = photoId).toReply { it }

        override suspend fun replacePhoto(submissionId: String, photoId: String, jpeg: ByteArray): ServerReply<Unit> =
            submissions.putPhoto(submissionId = submissionId, photoId = photoId, jpeg = jpeg).toReply { }
    }
