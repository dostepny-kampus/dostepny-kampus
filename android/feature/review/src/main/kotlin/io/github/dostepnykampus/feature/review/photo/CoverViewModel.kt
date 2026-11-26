/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.review.photo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.dostepnykampus.core.data.di.ComputeContext
import io.github.dostepnykampus.core.data.review.Reviews
import io.github.dostepnykampus.core.data.server.ServerReply
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/** Bajty [jpeg] zdjęcia pobranego do zasłaniania; równe innemu zdjęciu o tych samych bajtach. */
class CoverPhoto(val jpeg: ByteArray) {
    /**
     * Zdjęcie z [covers] wypalonymi w pikselach.
     *
     * @return bajty nowego zdjęcia JPEG; `null`, gdy [jpeg] nie jest obrazem
     */
    fun burned(covers: List<Cover>): ByteArray? =
        try {
            burnCovers(jpeg = jpeg, covers = covers)
        } catch (_: IllegalArgumentException) {
            null
        }

    override fun equals(other: Any?): Boolean = other is CoverPhoto && jpeg.contentEquals(other.jpeg)

    override fun hashCode(): Int = jpeg.contentHashCode()
}

/** Stan zasłaniania zdjęcia z R2. Wymagania: BR-15, NFR-25, NFR-53 */
sealed interface CoverUiState {
    /** Zdjęcie jeszcze się wczytuje. */
    data object Loading : CoverUiState

    /**
     * Zdjęcie do zasłaniania.
     *
     * @property photo zdjęcie z serwera bez zasłon
     * @property covers zasłony postawione przez Weryfikatora
     * @property isSaving „Zapisujemy zdjęcie…” zamiast „Zapisz zdjęcie”
     * @property isOffline zapis nie doszedł do serwera bez internetu; zasłony zostają
     */
    data class Ready(
        val photo: CoverPhoto,
        val covers: Covers = Covers(),
        val isSaving: Boolean = false,
        val isOffline: Boolean = false,
    ) : CoverUiState

    /** Bez internetu przy wczytywaniu. */
    data object Offline : CoverUiState

    /** Serwer odpowiedział błędem albo nie zna zdjęcia. */
    data object ServerError : CoverUiState

    /** Serwer odmówił, bo osoba nie ma już roli Weryfikatora albo zgłoszenie nie jest jej. */
    data object Forbidden : CoverUiState
}

/**
 * Zasłanianie twarzy i tablic na zdjęciu zgłoszenia: zasłony po dotknięciu i przyciskach, zapis zdjęcia
 * z zasłonami wypalonymi w pikselach; oryginał nie opuszcza telefonu.
 *
 * Wymagania: BR-15, NFR-25, NFR-53
 *
 * @param submissionId identyfikator sprawdzanego zgłoszenia
 * @param photoId identyfikator zasłanianego zdjęcia
 * @param reviews sprawdzanie zgłoszeń na serwerze
 * @param computeContext kontekst wypalania zasłon poza wątkiem interfejsu
 */
@HiltViewModel(assistedFactory = CoverViewModel.Factory::class)
class CoverViewModel
    @AssistedInject
    constructor(
        @Assisted("submissionId") private val submissionId: String,
        @Assisted("photoId") private val photoId: String,
        private val reviews: Reviews,
        @ComputeContext private val computeContext: CoroutineContext,
    ) : ViewModel() {
        private val current = MutableStateFlow<CoverUiState>(CoverUiState.Loading)

        /** Stan ekranu; zmienia się po wczytaniu, po każdej czynności i po zapisie. */
        val state: StateFlow<CoverUiState> = current

        init {
            load()
        }

        /** „Spróbuj ponownie”: czyta zdjęcie od nowa. */
        fun refresh() {
            current.value = CoverUiState.Loading
            load()
        }

        /** Dotknięcie zdjęcia w punkcie ([x], [y]) we współrzędnych względnych, od lewej i od góry. */
        fun tap(x: Float, y: Float) {
            change { it.tap(x = x, y = y) }
        }

        /** „Większy” dla zaznaczonej zasłony. */
        fun bigger() {
            change(Covers::bigger)
        }

        /** „Mniejszy” dla zaznaczonej zasłony. */
        fun smaller() {
            change(Covers::smaller)
        }

        /** „Usuń zasłonę” dla zaznaczonej zasłony. */
        fun remove() {
            change(Covers::remove)
        }

        /**
         * „Zapisz zdjęcie”: wypala zasłony i wysyła zdjęcie; bez zasłon wraca bez wysyłania.
         *
         * @param onSaved powrót do R2 po zapisie
         */
        fun save(onSaved: () -> Unit) {
            val shown = current.value as? CoverUiState.Ready ?: return
            when {
                shown.isSaving -> Unit
                shown.covers.items.isEmpty() -> onSaved()
                else -> send(shown, onSaved)
            }
        }

        private fun send(shown: CoverUiState.Ready, onSaved: () -> Unit) {
            current.value = shown.copy(isSaving = true, isOffline = false)
            viewModelScope.launch {
                val burned = withContext(computeContext) { shown.photo.burned(covers = shown.covers.items) }
                if (burned == null) {
                    current.value = CoverUiState.ServerError
                } else {
                    upload(shown = shown, jpeg = burned, onSaved = onSaved)
                }
            }
        }

        private suspend fun upload(shown: CoverUiState.Ready, jpeg: ByteArray, onSaved: () -> Unit) {
            when (val reply = reviews.replacePhoto(submissionId = submissionId, photoId = photoId, jpeg = jpeg)) {
                is ServerReply.Ok -> onSaved()
                ServerReply.Offline -> current.value = shown.copy(isOffline = true)
                ServerReply.Missing, is ServerReply.Fault -> current.value = reply.failure()
            }
        }

        private fun load() {
            viewModelScope.launch {
                val reply = reviews.photo(submissionId = submissionId, photoId = photoId)
                val photo = (reply as? ServerReply.Ok)?.value
                current.value = if (photo != null) CoverUiState.Ready(photo = CoverPhoto(photo)) else reply.failure()
            }
        }

        private fun change(step: (Covers) -> Covers) {
            val shown = current.value as? CoverUiState.Ready ?: return
            if (!shown.isSaving) current.value = shown.copy(covers = step(shown.covers))
        }

        private fun ServerReply<*>.failure(): CoverUiState =
            when (this) {
                ServerReply.Offline -> CoverUiState.Offline
                is ServerReply.Fault -> if (code == FORBIDDEN) CoverUiState.Forbidden else CoverUiState.ServerError
                is ServerReply.Ok, ServerReply.Missing -> CoverUiState.ServerError
            }

        /** Tworzy [CoverViewModel] dla zdjęcia z trasy zasłaniania. */
        @AssistedFactory
        interface Factory {
            /** Zasłanianie zdjęcia [photoId] zgłoszenia [submissionId]. */
            fun create(
                @Assisted("submissionId") submissionId: String,
                @Assisted("photoId") photoId: String,
            ): CoverViewModel
        }

        private companion object {
            const val FORBIDDEN = 403
        }
    }
