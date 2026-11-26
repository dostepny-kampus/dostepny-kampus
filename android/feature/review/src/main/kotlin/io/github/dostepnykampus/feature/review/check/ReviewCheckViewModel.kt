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
import io.github.dostepnykampus.core.data.building.NearestEntrance
import io.github.dostepnykampus.core.data.building.ServerEntrances
import io.github.dostepnykampus.core.data.review.Reviews
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.model.building.EntranceDistance
import io.github.dostepnykampus.core.model.review.ReviewDecision
import io.github.dostepnykampus.core.model.review.ReviewOutcome
import io.github.dostepnykampus.core.model.review.SimilarSubmission
import io.github.dostepnykampus.core.model.submission.BarrierType
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.core.model.submission.Submission
import io.github.dostepnykampus.core.model.submission.SubmissionField
import io.github.dostepnykampus.core.model.submission.fieldsChanged
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Komunikat dla Weryfikatora nad formularzem. Wymagania: US10.2, US10.8, BR-3, BR-11 */
enum class CheckNotice {
    /** Brakuje powodu odrzucenia. */
    REASON_MISSING,

    /** Zgłoszenie szybkie nie ma jeszcze typu bariery albo uciążliwości. */
    TYPE_MISSING,

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
     * @property quickType typ bariery nadany zgłoszeniu szybkiemu; `null`, zanim Weryfikator go wybierze
     * @property quickSeverity uciążliwość nadana zgłoszeniu szybkiemu; `null`, zanim Weryfikator ją wybierze
     * @property missingAttempts liczba prób przyjęcia zgłoszenia szybkiego bez typu albo uciążliwości
     * @property nearest najbliższe wejście budynku zgłoszenia; `null` bez budynku, wejść albo katalogu
     * @property changed pola, którymi nowa wersja różni się od wcześniej pokazanej; pusta lista bez nowej wersji
     * @property previous poprzednie zgłoszenie zmiany „bariera usunięta”; `null` przy innej zmianie
     * @property current obecne oznaczenie wejścia przy zmianie oznaczenia; `null` przy innym zgłoszeniu
     */
    data class Ready(
        val submission: Submission,
        val photoUrls: List<String>,
        val similar: List<SimilarSubmission>,
        val notice: CheckNotice? = null,
        val similarPhotos: Map<String, String> = emptyMap(),
        val quickType: BarrierType? = null,
        val quickSeverity: Severity? = null,
        val missingAttempts: Int = 0,
        val nearest: EntranceDistance? = null,
        val changed: List<SubmissionField> = emptyList(),
        val previous: PreviousSubmission? = null,
        val current: CurrentMarking? = null,
    ) : CheckUiState

    /** Bez internetu przy wczytywaniu. */
    data object Offline : CheckUiState

    /** Serwer odpowiedział błędem albo nie zna zgłoszenia. */
    data object ServerError : CheckUiState

    /** Serwer odmówił, bo osoba nie ma już roli Weryfikatora albo zgłoszenie nie jest jej. */
    data object Forbidden : CheckUiState
}

/**
 * R2 „Sprawdzenie zgłoszenia”: zgłoszenie, podobne zgłoszenia, poprzednie zgłoszenie zmiany „bariera usunięta”
 * i obecne oznaczenie przy zmianie oznaczenia wejścia przy otwarciu, przyjęcie i odrzucenie z powodem.
 *
 * Wymagania: US10.1, US10.2, US10.6, US10.8, BR-3, BR-11, BR-13, BR-16, BR-19, BR-24, NFR-50
 *
 * @param submissionId identyfikator sprawdzanego zgłoszenia
 * @param reviews sprawdzanie zgłoszeń na serwerze
 * @param nearestEntrance najbliższe wejście budynku do miejsca zgłoszenia słowami
 * @param serverEntrances wejścia budynku z serwera z obecnym oznaczeniem
 */
@HiltViewModel(assistedFactory = ReviewCheckViewModel.Factory::class)
class ReviewCheckViewModel
    @AssistedInject
    constructor(
        @Assisted private val submissionId: String,
        private val reviews: Reviews,
        private val nearestEntrance: NearestEntrance,
        private val serverEntrances: ServerEntrances,
    ) : ViewModel() {
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
         * „Przyjmij”. Zgłoszenie szybkie bez typu albo uciążliwości daje [CheckNotice.TYPE_MISSING].
         *
         * @param onDone powrót do R1 po zapisanej decyzji
         */
        fun accept(onDone: () -> Unit) {
            val shown = current.value as? CheckUiState.Ready ?: return
            val decision = shown.acceptance()
            if (decision == null) {
                val attempts = shown.missingAttempts + 1
                current.value = shown.copy(notice = CheckNotice.TYPE_MISSING, missingAttempts = attempts)
            } else {
                decide(decision, onDone)
            }
        }

        /** Typ bariery dla zgłoszenia szybkiego. Wymagania: US10.3, BR-14 */
        fun pickType(type: BarrierType) {
            val shown = current.value as? CheckUiState.Ready ?: return
            current.value = shown.picked(type = type)
        }

        /** Uciążliwość dla zgłoszenia szybkiego. Wymagania: US10.3 */
        fun pickSeverity(severity: Severity) {
            val shown = current.value as? CheckUiState.Ready ?: return
            current.value = shown.picked(severity = severity)
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
                val shown = current.value
                if (shown is CheckUiState.Ready) {
                    when (val reply = reviews.decide(submissionId, decision)) {
                        is ServerReply.Ok -> decided(reply.value, shown, onDone)
                        ServerReply.Offline -> current.value = shown.copy(notice = CheckNotice.OFFLINE)
                        ServerReply.Missing, is ServerReply.Fault -> current.value = reply.checkFailure()
                    }
                }
            }
        }

        private suspend fun decided(outcome: ReviewOutcome, shown: CheckUiState.Ready, onDone: () -> Unit) {
            when (outcome) {
                is ReviewOutcome.Decided -> {
                    onDone()
                }

                is ReviewOutcome.Changed -> {
                    val server = outcome.server
                    current.value =
                        ready(server, shown.similar, CheckNotice.CHANGED).copy(
                            quickType = shown.quickType,
                            quickSeverity = shown.quickSeverity,
                            nearest = shown.nearest,
                            changed = fieldsChanged(previous = shown.submission.content, current = server.content),
                            previous = shown.previous,
                            current = serverEntrances.currentMarking(server),
                        )
                }
            }
        }

        private fun load() {
            viewModelScope.launch { current.value = read() }
        }

        private suspend fun read(): CheckUiState {
            val found = reviews.submission(submissionId)
            if (found !is ServerReply.Ok) return found.checkFailure()
            val similar = reviews.similar(submissionId)
            val content = found.value.content
            return if (similar is ServerReply.Ok) {
                val nearest = nearestEntrance.find(buildingSymbol = content.buildingSymbol, point = content.location)
                ready(found.value, similar.value, notice = null).copy(
                    nearest = nearest,
                    previous = reviews.previousOf(content),
                    current = serverEntrances.currentMarking(found.value),
                )
            } else {
                similar.checkFailure()
            }
        }

        private fun ready(
            submission: Submission,
            similar: List<SimilarSubmission>,
            notice: CheckNotice?,
        ): CheckUiState.Ready =
            CheckUiState.Ready(
                submission = submission,
                photoUrls =
                    submission.content.photoIds.map { photo ->
                        reviews.photoUrl(submissionId = submission.id, photoId = photo)
                    },
                similar = similar,
                notice = notice,
                similarPhotos = similar.mapNotNull { near -> reviews.firstPhotoOf(near) }.toMap(),
            )

        /** Tworzy [ReviewCheckViewModel] dla zgłoszenia z trasy R2. */
        @AssistedFactory
        interface Factory {
            /** Ekran R2 zgłoszenia [submissionId]. */
            fun create(submissionId: String): ReviewCheckViewModel
        }
    }
