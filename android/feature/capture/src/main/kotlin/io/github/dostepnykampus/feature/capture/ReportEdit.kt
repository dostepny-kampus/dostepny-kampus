/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import io.github.dostepnykampus.core.data.submissions.PendingPhotoDraft
import io.github.dostepnykampus.core.model.BarrierType
import io.github.dostepnykampus.core.model.Severity

/** Co ekran C2 robi po „Zapisz” (US7). */
enum class ReportStage {
    /** Formularz otwarty. */
    EDITING,

    /** Zapis trwa; „Zapisz” jest nieczynne, żeby nie zapisać dwa razy. */
    SAVING,

    /** „Za mało miejsca w telefonie” z „Spróbuj ponownie” (NFR-7). */
    SAVE_FAILED,

    /** Kolejka odrzuciła wpis, bo zdjęcie nie pasuje do treści; pola zostają, aplikacja działa dalej (NFR-50). */
    SAVE_REJECTED,

    /** Kolejka odrzuciła poprawkę, bo telefon nie zna wersji zgłoszenia na serwerze; pola zostają (BR-10, NFR-50). */
    VERSION_UNKNOWN,

    /** Pytanie „Wyjść bez zapisu?” po „Wstecz” (US7.4, NFR-23). */
    EXIT_ASKED,

    /** Zapisane; ekran przechodzi do C3, a C1 i C2 znikają z historii (NFR-54). */
    SAVED,

    /** Wyjście bez zapisu. */
    LEFT,

    /** „Zmień miejsce”: pola czekają w szkicu na nowe miejsce z C1 (US7.4). */
    PLACE_CHANGED,
}

/**
 * Stan ekranu C2.
 *
 * @property form bieżące pola
 * @property photos zdjęcia z plikami, w kolejności [ReportForm.photoIds]
 * @property errors braki pokazywane dopiero po pierwszym „Zapisz” (US7.1)
 * @property stage etap ekranu
 * @property isSaveAttempted czy „Zapisz” było już naciśnięte; od tej chwili braki liczą się na bieżąco
 */
data class ReportUiState(
    val form: ReportForm,
    val photos: List<PendingPhotoDraft> = emptyList(),
    val errors: List<ReportFormError> = emptyList(),
    val stage: ReportStage = ReportStage.EDITING,
    val isSaveAttempted: Boolean = false,
)

/** Zmiana jednego pola na ekranie C2. */
sealed interface ReportEdit {
    /**
     * Nowe miejsce po „Zmień miejsce” na C1; piętro zostaje tylko w tym samym budynku (US7.4).
     *
     * @property place miejsce z ponownego pomiaru
     */
    data class Place(val place: ReportPlace) : ReportEdit

    /**
     * Wybór piętra (US7.2).
     *
     * @property floor piętro albo `null`
     */
    data class Floor(val floor: ReportFloor?) : ReportEdit

    /**
     * Wybór typu bariery (US7.1).
     *
     * @property type typ bariery
     */
    data class Type(val type: BarrierType) : ReportEdit

    /**
     * Wybór uciążliwości (US7.1).
     *
     * @property severity uciążliwość
     */
    data class Rating(val severity: Severity) : ReportEdit

    /**
     * Zmiana opisu (US7.3).
     *
     * @property text wpisany tekst
     */
    data class Description(val text: String) : ReportEdit

    /**
     * Zdjęcie z aparatu (US7.1, BR-7).
     *
     * @property photo plik i identyfikator zdjęcia
     */
    data class Photo(val photo: PendingPhotoDraft) : ReportEdit
}

/**
 * Stan po zmianie pola. Po pierwszym „Zapisz” braki liczą się na bieżąco: znikają po uzupełnieniu pola
 * i wracają, gdy zmiana tworzy nowy, np. typ TB-29 bez opisu. Piętra poza budynkiem nie ma (BR-G4).
 */
internal fun ReportUiState.edited(change: ReportEdit): ReportUiState {
    val photos = if (change is ReportEdit.Photo) photos + change.photo else photos
    val form =
        when (change) {
            is ReportEdit.Place -> form.movedTo(change.place)
            is ReportEdit.Floor -> form.copy(floor = change.floor.takeIf { form.place.buildingSymbol != null })
            is ReportEdit.Type -> form.copy(barrierType = change.type)
            is ReportEdit.Rating -> form.copy(severity = change.severity)
            is ReportEdit.Description -> form.copy(description = change.text)
            is ReportEdit.Photo -> form.copy(photoIds = photos.map(PendingPhotoDraft::photoId))
        }
    return copy(form = form, photos = photos, errors = if (isSaveAttempted) form.errors() else emptyList())
}

/** Pola formularza w nowym miejscu; piętro zostaje tylko w tym samym budynku (US7.4, BR-G4). */
internal fun ReportForm.movedTo(next: ReportPlace): ReportForm =
    copy(place = next, floor = floor.takeIf { next.buildingSymbol == place.buildingSymbol })
