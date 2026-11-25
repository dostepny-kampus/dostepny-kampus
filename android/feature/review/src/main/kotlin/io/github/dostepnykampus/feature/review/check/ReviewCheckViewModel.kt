/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.review.check

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.dostepnykampus.core.data.review.Reviews
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.model.review.ReviewDecision
import io.github.dostepnykampus.core.model.review.ReviewOutcome
import io.github.dostepnykampus.core.model.review.SimilarSubmission
import io.github.dostepnykampus.core.model.submission.Submission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Komunikat nad decyzją R2. Wymagania: US10.2, US10.8, BR-3, BR-11 */
enum class CheckNotice {
    /** „Podaj powód odrzucenia, aby autor wiedział, co poprawić.” */
    REASON_MISSING,

    /** „Autor zmienił zgłoszenie. Sprawdź nową wersję przed decyzją.” */
    CHANGED,

    /** Decyzja wymaga internetu, a zgłoszenie czeka w „Wziętych przeze mnie”. */
    OFFLINE,
}

/** Stan ekranu R2. Wymagania: US10.1, US10.8, NFR-50 */
sealed interface CheckUiState {
    /** Zgłoszenie jeszcze się wczytuje. */
    data object Loading : CheckUiState

    /**
     * Zgłoszenie do decyzji.
     *
     * @property submission zgłoszenie w wersji, o której Weryfikator decyduje
     * @property photoUrls adresy zdjęć zgłoszenia w kolejności `photoIds`
     * @property similar zgłoszenia do 10 m, od najbliższego
     * @property notice komunikat nad decyzją; `null` bez komunikatu
     * @property similarPhotos adres pierwszego zdjęcia podobnego zgłoszenia według jego identyfikatora
     */
    data class Ready(
        val submission: Submission,
        val photoUrls: List<String>,
        val similar: List<SimilarSubmission>,
        val notice: CheckNotice? = null,
        val similarPhotos: Map<String, String> = emptyMap(),
    ) : CheckUiState

    /** Bez internetu przy wczytywaniu. */
    data object Offline : CheckUiState

    /** Serwer odpowiedział błędem albo nie zna zgłoszenia. */
    data object ServerError : CheckUiState

    /** Serwer odmówił, bo osoba nie ma już roli Weryfikatora albo zgłoszenie nie jest jej. */
    data object Forbidden : CheckUiState
}

/**
 * R2 „Sprawdzenie zgłoszenia”: zgłoszenie i podobne zgłoszenia przy otwarciu, przyjęcie i odrzucenie z powodem.
 *
 * Wymagania: US10.1, US10.2, US10.8, BR-3, BR-11, BR-13, BR-16, NFR-50
 *
 * @param submissionId identyfikator sprawdzanego zgłoszenia
 * @param reviews sprawdzanie zgłoszeń na serwerze
 */
@HiltViewModel(assistedFactory = ReviewCheckViewModel.Factory::class)
class ReviewCheckViewModel
    @AssistedInject
    constructor(@Assisted private val submissionId: String, private val reviews: Reviews) : ViewModel() {
        private val current = MutableStateFlow<CheckUiState>(CheckUiState.Loading)

        /** Stan ekranu; zmienia się po otwarciu, po [refresh] i po decyzji. */
        val state: StateFlow<CheckUiState> = current

        init {
            load()
        }

        /** „Spróbuj ponownie”: czyta zgłoszenie i podobne od nowa. */
        fun refresh() {
            current.value = CheckUiState.Loading
            load()
        }

        /**
         * „Przyjmij”.
         *
         * TODO(#3762): typ i uciążliwość ustalone dla zgłoszenia szybkiego.
         *
         * @param onDone powrót do R1 po zapisanej decyzji
         */
        fun accept(onDone: () -> Unit) {
            val shown = current.value as? CheckUiState.Ready ?: return
            decide(ReviewDecision.Accept(version = shown.submission.version), onDone)
        }

        /**
         * „Odrzuć” z powodem widocznym dla autora; pusty powód daje [CheckNotice.REASON_MISSING].
         *
         * @param reason powód odrzucenia
         * @param duplicateOf wcześniejsze zgłoszenie przy „Duplikat”; `null` przy innym powodzie
         * @param onDone powrót do R1 po zapisanej decyzji
         */
        fun reject(reason: String, duplicateOf: String?, onDone: () -> Unit) {
            val shown = current.value as? CheckUiState.Ready ?: return
            val version = shown.submission.version
            val text = reason.trim()
            when {
                text.isEmpty() -> {
                    current.value = shown.copy(notice = CheckNotice.REASON_MISSING)
                }

                duplicateOf == null -> {
                    decide(ReviewDecision.Reject(version = version, reason = text), onDone)
                }

                else -> {
                    val duplicate =
                        ReviewDecision.RejectDuplicate(version = version, reason = text, duplicateOf = duplicateOf)
                    decide(duplicate, onDone)
                }
            }
        }

        private fun decide(decision: ReviewDecision, onDone: () -> Unit) {
            viewModelScope.launch {
                val shown = current.value as? CheckUiState.Ready ?: return@launch
                when (val reply = reviews.decide(submissionId, decision)) {
                    is ServerReply.Ok -> decided(reply.value, shown, onDone)
                    ServerReply.Offline -> current.value = shown.copy(notice = CheckNotice.OFFLINE)
                    else -> current.value = reply.failure()
                }
            }
        }

        private fun decided(outcome: ReviewOutcome, shown: CheckUiState.Ready, onDone: () -> Unit) {
            when (outcome) {
                is ReviewOutcome.Decided -> onDone()
                is ReviewOutcome.Changed -> current.value = ready(outcome.server, shown.similar, CheckNotice.CHANGED)
            }
        }

        private fun load() {
            viewModelScope.launch { current.value = read() }
        }

        private suspend fun read(): CheckUiState =
            when (val found = reviews.submission(submissionId)) {
                is ServerReply.Ok -> {
                    when (val similar = reviews.similar(submissionId)) {
                        is ServerReply.Ok -> ready(found.value, similar.value, notice = null)
                        else -> similar.failure()
                    }
                }

                else -> {
                    found.failure()
                }
            }

        private fun ready(
            submission: Submission,
            similar: List<SimilarSubmission>,
            notice: CheckNotice?,
        ): CheckUiState =
            CheckUiState.Ready(
                submission = submission,
                photoUrls =
                    submission.content.photoIds.map { photo ->
                        reviews.photoUrl(submissionId = submission.id, photoId = photo)
                    },
                similar = similar,
                notice = notice,
                similarPhotos = similar.mapNotNull(::firstPhoto).toMap(),
            )

        private fun firstPhoto(near: SimilarSubmission): Pair<String, String>? {
            val id = near.submission.id
            val photo = near.submission.content.photoIds.firstOrNull() ?: return null
            return id to reviews.photoUrl(submissionId = id, photoId = photo)
        }

        private fun ServerReply<*>.failure(): CheckUiState =
            when (this) {
                ServerReply.Offline -> CheckUiState.Offline
                is ServerReply.Fault -> if (code == FORBIDDEN) CheckUiState.Forbidden else CheckUiState.ServerError
                else -> CheckUiState.ServerError
            }

        /** Tworzy [ReviewCheckViewModel] dla zgłoszenia z trasy R2. */
        @AssistedFactory
        interface Factory {
            /** Ekran R2 zgłoszenia [submissionId]. */
            fun create(submissionId: String): ReviewCheckViewModel
        }

        private companion object {
            const val FORBIDDEN = 403
        }
    }
