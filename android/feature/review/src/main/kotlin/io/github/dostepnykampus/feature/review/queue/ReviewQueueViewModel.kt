/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.review.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.dostepnykampus.core.data.review.ReviewQueue
import io.github.dostepnykampus.core.data.review.Reviews
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.model.review.ReviewClaim
import io.github.dostepnykampus.core.model.submission.Submission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Stan ekranu R1. Wymagania: US10.4, US10.7, NFR-50 */
sealed interface ReviewQueueUiState {
    /** „Wczytujemy zgłoszenia…”. */
    data object Loading : ReviewQueueUiState

    /**
     * Kolejka z serwera.
     *
     * @property waiting zgłoszenia do wzięcia, od najdawniej wysłanego
     * @property mine zgłoszenia wzięte przez tego Weryfikatora, bez decyzji
     * @property isTaken czy ostatnie wzięcie się nie udało, bo zgłoszenie wziął ktoś inny
     */
    data class Ready(val waiting: List<Submission>, val mine: List<Submission>, val isTaken: Boolean = false) :
        ReviewQueueUiState

    /** Bez internetu: kolejka jest tylko na serwerze. */
    data object Offline : ReviewQueueUiState

    /** Serwer odpowiedział błędem albo treścią spoza kontraktu. */
    data object ServerError : ReviewQueueUiState

    /** Serwer odmówił, bo osoba nie ma już roli Weryfikatora. */
    data object Forbidden : ReviewQueueUiState
}

/**
 * R1 „Zgłoszenia do sprawdzenia”: odczyt kolejki przy otwarciu i na „Spróbuj ponownie” oraz wzięcie zgłoszenia.
 *
 * Wymagania: US10.4, US10.7, BR-11, NFR-50
 *
 * @param reviews sprawdzanie zgłoszeń na serwerze
 */
@HiltViewModel
class ReviewQueueViewModel
    @Inject
    constructor(private val reviews: Reviews) : ViewModel() {
        private val current = MutableStateFlow<ReviewQueueUiState>(ReviewQueueUiState.Loading)

        /** Stan kolejki; zmienia się po otwarciu, po [refresh] i po [claim]. */
        val state: StateFlow<ReviewQueueUiState> = current

        init {
            load()
        }

        /** „Spróbuj ponownie”: czyta kolejkę od nowa. */
        fun refresh() {
            current.value = ReviewQueueUiState.Loading
            load()
        }

        /** Powrót do R1 z R2: czyta kolejkę od nowa, gdy jest już pokazana; pierwszy odczyt idzie przy otwarciu. */
        fun reopen() {
            if (current.value is ReviewQueueUiState.Ready) refresh()
        }

        /**
         * „Weź do sprawdzenia”. Zgłoszenie wzięte przez kogoś innego znika z listy z komunikatem.
         *
         * @param submissionId identyfikator zgłoszenia
         * @param onClaimed otwarcie wziętego zgłoszenia; argument to jego identyfikator
         */
        fun claim(submissionId: String, onClaimed: (String) -> Unit) {
            viewModelScope.launch {
                when (val reply = reviews.claim(submissionId)) {
                    is ServerReply.Ok -> claimed(reply.value, submissionId, onClaimed)
                    else -> current.value = reply.failure()
                }
            }
        }

        private fun claimed(claim: ReviewClaim, submissionId: String, onClaimed: (String) -> Unit) {
            val shown = current.value
            if (shown is ReviewQueueUiState.Ready) current.value = shown.after(claim, submissionId)
            if (claim is ReviewClaim.Claimed) onClaimed(submissionId)
        }

        private fun load() {
            viewModelScope.launch {
                current.value =
                    when (val reply = reviews.queue()) {
                        is ServerReply.Ok -> reply.value.toState()
                        else -> reply.failure()
                    }
            }
        }

        private fun ReviewQueue.toState(): ReviewQueueUiState =
            ReviewQueueUiState.Ready(
                waiting = waiting.sortedBy { it.createdAt },
                mine = mine.sortedBy { it.createdAt },
            )

        private fun ServerReply<*>.failure(): ReviewQueueUiState =
            when (this) {
                ServerReply.Offline -> ReviewQueueUiState.Offline
                is ServerReply.Fault -> faultState(code)
                else -> ReviewQueueUiState.ServerError
            }

        private fun faultState(code: Int): ReviewQueueUiState =
            if (code == FORBIDDEN) ReviewQueueUiState.Forbidden else ReviewQueueUiState.ServerError

        private companion object {
            const val FORBIDDEN = 403
        }
    }

private fun ReviewQueueUiState.Ready.after(claim: ReviewClaim, submissionId: String): ReviewQueueUiState.Ready {
    val left = waiting.filterNot { it.id == submissionId }
    return when (claim) {
        is ReviewClaim.Claimed -> copy(waiting = left, mine = mine + claim.submission, isTaken = false)
        ReviewClaim.Taken -> copy(waiting = left, isTaken = true)
    }
}
