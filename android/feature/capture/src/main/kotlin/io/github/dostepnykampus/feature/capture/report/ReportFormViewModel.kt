/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.report

import android.database.sqlite.SQLiteException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.dostepnykampus.core.data.buildings.BuildingFloors
import io.github.dostepnykampus.core.data.submissions.EditedSubmission
import io.github.dostepnykampus.core.data.submissions.PendingPhotoDraft
import io.github.dostepnykampus.core.data.submissions.SaveRefusal
import io.github.dostepnykampus.core.data.submissions.SaveRefusedException
import io.github.dostepnykampus.core.model.FloorList
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.feature.capture.measure.place
import io.github.dostepnykampus.feature.capture.quick.content
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/**
 * Zapis zgłoszenia w kolejce telefonu; w aplikacji to `SubmissionQueue.save` albo `saveEdit`.
 *
 * Wymagania: NFR-5, US9.1
 */
fun interface ReportSaver {
    /**
     * Zapisuje treść i zdjęcia; rzuca [IOException] albo [SQLiteException], gdy telefon nie może zapisać.
     *
     * Wymagania: NFR-7, US8.3
     *
     * @param content treść zgłoszenia
     * @param photos zdjęcia zgłoszenia
     * @param edited poprawiane zgłoszenie z C3 albo `null` dla nowego
     */
    suspend fun save(content: SubmissionContent, photos: List<PendingPhotoDraft>, edited: EditedSubmission?)
}

/**
 * Model widoku formularza C2 „Opisz barierę”. W czasie zapisu i po nim etap ustala tylko zapis, więc
 * nie ma dwóch zgłoszeń. Pełna pamięć przy zapisie w bazie Room kończy się etapem nieudanego zapisu.
 * Po otwarciu w budynku pobiera piętra z planu PWr; bez nich zostaje wpisanie piętra.
 *
 * Wymagania: US7.1, US7.2, US7.3, US7.4, NFR-7, BR-G1
 *
 * @param floors piętra budynku z serwera; domyślnie brak, jak budynek podstawowy
 * @param saver zapis w kolejce telefonu
 */
@HiltViewModel
class ReportFormViewModel
    @Inject
    constructor(
        private val floors: BuildingFloors = BuildingFloors { null },
        private val saver: ReportSaver,
    ) : ViewModel() {
        private val state = MutableStateFlow<ReportUiState?>(null)
        private var initial: ReportForm? = null
        private var draft = ReportDraft()

        /** Stan ekranu; `null`, dopóki C1 nie poda miejsca. */
        val uiState: StateFlow<ReportUiState?> = state.asStateFlow()

        /**
         * Otwiera nowy formularz albo edycję; drugie wywołanie nie kasuje wpisanych pól. Po „Zmień miejsce” bierze
         * odłożone pola z nowym miejscem z [form]. Poprawka z C3 podaje zapisane [photos].
         *
         * Wymagania: US7.4, US8.3
         */
        fun open(form: ReportForm, draft: ReportDraft = this.draft, photos: List<PendingPhotoDraft> = emptyList()) {
            val current = state.value
            if (current == null) {
                initial = form
                this.draft = draft
                keep(draft.restore(form, photos))
                form.place.buildingSymbol?.let(::loadFloors)
            } else if (this.draft !== draft) {
                this.draft = draft
                if (current.stage !in StagesWithoutNewDraft) draft.keep(current)
            }
        }

        /** „Zmień miejsce”: odkłada pola i wraca do C1 bez pytania, bo nic nie ginie. Wymagania: US7.4 */
        fun changePlace() {
            val current = state.value ?: return
            if (current.isSavingOrSaved) return
            state.value = current.copy(stage = ReportStage.PLACE_CHANGED)
        }

        /** Zmiana jednego pola formularza. Wymagania: US7.1, US7.2, US7.3, US7.4 */
        fun edit(change: ReportEdit) {
            val current = state.value ?: return
            if (!current.isSavingOrSaved) keep(current.edited(change))
        }

        /** „Zapisz”: przy brakach pokazuje je, inaczej zapisuje w kolejce. Wymagania: US7.1, NFR-5 */
        fun save() {
            val current = state.value ?: return
            if (current.isSavingOrSaved) return
            when (val check = current.form.check()) {
                is ReportCheck.Incomplete -> keep(current.copy(errors = check.errors, isSaveAttempted = true))
                is ReportCheck.Complete -> store(current, check.content)
            }
        }

        /**
         * „Wstecz”: bez zmian wychodzi od razu, ze zmianami pyta; w czasie zapisu nic nie robi.
         *
         * Wymagania: US7.4, NFR-23
         */
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
            val current = state.value ?: return
            if (current.isSavingOrSaved) return
            val stage = next(current)
            if (stage == ReportStage.LEFT) draft.clear()
            state.value = current.copy(stage = stage)
        }

        private fun loadFloors(symbol: String) {
            viewModelScope.launch {
                val plan = floors.floors(symbol)?.toPlanFloors()
                val current = state.value
                if (plan != null && current != null && !current.isSavingOrSaved) {
                    initial = initial?.withPlan(symbol, plan)
                    keep(current.copy(form = current.form.withPlan(symbol, plan)))
                }
            }
        }

        private fun keep(current: ReportUiState) {
            draft.keep(current)
            state.value = current
        }

        private fun store(current: ReportUiState, content: SubmissionContent) {
            state.value = current.copy(errors = emptyList(), stage = ReportStage.SAVING, isSaveAttempted = true)
            viewModelScope.launch {
                val stage =
                    try {
                        saver.save(content, current.photos, current.form.editedSubmission)
                        draft.clear()
                        ReportStage.SAVED
                    } catch (_: IOException) {
                        ReportStage.SAVE_FAILED
                    } catch (_: SQLiteException) {
                        ReportStage.SAVE_FAILED
                    } catch (refused: SaveRefusedException) {
                        refused.reason.toStage()
                    }
                state.update { it?.copy(stage = stage) }
            }
        }
    }

/**
 * Formularz z piętrami z planu, gdy miejsce jest w budynku [symbol]. Piętro wpisane liczbą przed pobraniem planu
 * znika, bo w budynku szczegółowym piętro wybiera się z etykiet PWr.
 *
 * Wymagania: US7.2
 */
internal fun ReportForm.withPlan(symbol: String, plan: PlanFloors): ReportForm =
    if (place.buildingSymbol != symbol) {
        this
    } else {
        copy(place = place.copy(floors = plan), floor = floor as? ReportFloor.Planned)
    }

/** Piętra z planu do formularza albo `null` dla budynku podstawowego z pustą listą. Wymagania: BR-G1 */
internal fun FloorList.toPlanFloors(): PlanFloors? =
    if (floors.isEmpty()) null else PlanFloors(items = floors.map { PlanFloor(id = it.id, label = it.label) }, source)

/** Etap formularza po odmowie zapisu: inny komunikat dla zdjęć, inny dla nieznanej wersji. Wymagania: NFR-50, BR-10 */
internal fun SaveRefusal.toStage(): ReportStage =
    when (this) {
        SaveRefusal.PHOTOS_MISMATCH -> ReportStage.SAVE_REJECTED
        SaveRefusal.UNKNOWN_VERSION -> ReportStage.VERSION_UNKNOWN
    }

private val StagesWithoutNewDraft = setOf(ReportStage.LEFT, ReportStage.PLACE_CHANGED, ReportStage.SAVED)

internal val ReportUiState.isSavingOrSaved: Boolean
    get() = stage == ReportStage.SAVING || stage == ReportStage.SAVED
