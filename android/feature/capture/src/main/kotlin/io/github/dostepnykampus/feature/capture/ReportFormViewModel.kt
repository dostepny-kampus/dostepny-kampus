/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import android.database.sqlite.SQLiteException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.dostepnykampus.core.data.submissions.EditedSubmission
import io.github.dostepnykampus.core.data.submissions.PendingPhotoDraft
import io.github.dostepnykampus.core.model.SubmissionContent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/** Zapis zgłoszenia w kolejce telefonu; w aplikacji to `SubmissionQueue.save` albo `saveEdit` (NFR-5, US9.1). */
fun interface ReportSaver {
    /**
     * Zapisuje treść i zdjęcia; rzuca [IOException] albo [SQLiteException], gdy telefon nie może zapisać (NFR-7).
     *
     * @param content treść zgłoszenia
     * @param photos zdjęcia zgłoszenia
     * @param edited poprawiane zgłoszenie z C3 albo `null` dla nowego (US8.3)
     */
    suspend fun save(content: SubmissionContent, photos: List<PendingPhotoDraft>, edited: EditedSubmission?)
}

/**
 * Model widoku formularza C2 „Opisz barierę” (US7.1–US7.4). W czasie zapisu i po nim etap ustala tylko zapis, więc
 * nie ma dwóch zgłoszeń. Pełna pamięć przy zapisie w bazie Room kończy się etapem nieudanego zapisu (NFR-7).
 */
@HiltViewModel
class ReportFormViewModel
    @Inject
    constructor(
        private val draft: ReportDraft = ReportDraft(),
        private val saver: ReportSaver,
    ) : ViewModel() {
        private val state = MutableStateFlow<ReportUiState?>(null)
        private var initial: ReportForm? = null

        /** Stan ekranu; `null`, dopóki C1 nie poda miejsca. */
        val uiState: StateFlow<ReportUiState?> = state.asStateFlow()

        /**
         * Otwiera nowy formularz albo edycję; drugie wywołanie nie kasuje wpisanych pól. Po „Zmień miejsce” bierze
         * odłożone pola z nowym miejscem z [form] (US7.4).
         */
        fun open(form: ReportForm) {
            if (state.value == null) {
                initial = form
                val kept = draft.take()
                state.value = kept?.copy(form = kept.form.movedTo(form.place)) ?: ReportUiState(form = form)
            }
        }

        /** „Zmień miejsce”: odkłada pola i wraca do C1 bez pytania, bo nic nie ginie (US7.4). */
        fun changePlace() {
            val current = state.value ?: return
            if (current.isSavingOrSaved) return
            draft.keep(current.copy(stage = ReportStage.EDITING))
            state.value = current.copy(stage = ReportStage.LEFT)
        }

        /** Zmiana jednego pola formularza (US7.1–US7.4). */
        fun edit(change: ReportEdit) {
            state.update { current ->
                if (current == null || current.isSavingOrSaved) current else current.edited(change)
            }
        }

        /** „Zapisz”: przy brakach pokazuje je, inaczej zapisuje w kolejce (US7.1, NFR-5). */
        fun save() {
            val current = state.value ?: return
            if (current.isSavingOrSaved) return
            when (val check = current.form.check()) {
                is ReportCheck.Incomplete -> state.value = current.copy(errors = check.errors, isSaveAttempted = true)
                is ReportCheck.Complete -> store(current, check.content)
            }
        }

        /** „Wstecz”: bez zmian wychodzi od razu, ze zmianami pyta (US7.4, NFR-23); w czasie zapisu nic nie robi. */
        fun back() {
            moveToWhileEditable { current -> if (current.form == initial) ReportStage.LEFT else ReportStage.EXIT_ASKED }
        }

        /** „Wyjdź bez zapisu”. */
        fun confirmExit() {
            moveToWhileEditable { ReportStage.LEFT }
        }

        /** „Wróć do formularza” albo zamknięcie komunikatu o błędzie zapisu. */
        fun stay() {
            moveToWhileEditable { ReportStage.EDITING }
        }

        private fun moveToWhileEditable(next: (ReportUiState) -> ReportStage) {
            state.update { current ->
                if (current == null || current.isSavingOrSaved) current else current.copy(stage = next(current))
            }
        }

        private fun store(current: ReportUiState, content: SubmissionContent) {
            state.value = current.copy(errors = emptyList(), stage = ReportStage.SAVING, isSaveAttempted = true)
            viewModelScope.launch {
                val stage =
                    try {
                        saver.save(content, current.photos, current.form.editedSubmission)
                        ReportStage.SAVED
                    } catch (_: IOException) {
                        ReportStage.SAVE_FAILED
                    } catch (_: SQLiteException) {
                        ReportStage.SAVE_FAILED
                    }
                state.update { it?.copy(stage = stage) }
            }
        }
    }

internal val ReportUiState.isSavingOrSaved: Boolean
    get() = stage == ReportStage.SAVING || stage == ReportStage.SAVED
