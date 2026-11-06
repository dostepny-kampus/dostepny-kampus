/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import io.github.dostepnykampus.core.model.BarrierType
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.Severity
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.core.model.SubmissionFloor
import io.github.dostepnykampus.core.model.SubmissionKind

/**
 * Miejsce bariery z pomiaru C1, pokazywane na górze formularza C2 (US7.4, US6.1).
 *
 * @property location uśredniony punkt pomiaru
 * @property accuracyMeters dokładność pomiaru w metrach
 * @property buildingSymbol budynek potwierdzony na C1 albo `null` poza budynkiem (BR-G4)
 * @property isManualCorrection czy punkt poprawiono ręcznie na planie piętra (US6.3)
 */
data class ReportPlace(
    val location: GeoPoint,
    val accuracyMeters: Double,
    val buildingSymbol: String?,
    val isManualCorrection: Boolean,
)

/** Piętro bariery w formularzu C2 (US7.2, BR-G1). */
sealed interface ReportFloor {
    /**
     * Piętro z etykiet planu PWr w budynku szczegółowym.
     *
     * @property floorId identyfikator piętra z danych PWr
     */
    data class Planned(val floorId: String) : ReportFloor

    /**
     * Piętro wpisane liczbą w budynku podstawowym; 0 to parter, liczby ujemne to piwnice.
     *
     * @property number numer piętra
     * @property isMezzanine czy to półpiętro
     */
    data class Numbered(val number: Int, val isMezzanine: Boolean) : ReportFloor
}

/** Brakujące pole formularza C2, w kolejności pól na ekranie (US7.1–US7.3, BR-7). */
enum class ReportFormError {
    /** Brak piętra w budynku; poza budynkiem piętra nie ma (BR-7, BR-G4). */
    MISSING_FLOOR,

    /** „Brakuje typu bariery”. */
    MISSING_BARRIER_TYPE,

    /** Nie wybrano uciążliwości. */
    MISSING_SEVERITY,

    /** Nie zrobiono zdjęcia. */
    MISSING_PHOTO,

    /** Brak opisu przy TB-29 „inna bariera”. */
    MISSING_DESCRIPTION,
}

/** Wynik sprawdzenia formularza przed „Zapisz”. */
sealed interface ReportCheck {
    /**
     * Formularz kompletny.
     *
     * @property content treść zgłoszenia do zapisania w kolejce
     */
    data class Complete(val content: SubmissionContent) : ReportCheck

    /**
     * Formularz z brakami; nic nie trafia do kolejki.
     *
     * @property errors braki w kolejności pól na ekranie
     */
    data class Incomplete(val errors: List<ReportFormError>) : ReportCheck
}

/**
 * Pola formularza C2 „Opisz barierę” (US7). Stan jest niezmienny: każda zmiana pola daje nową kopię,
 * więc porównanie z wartościami startowymi mówi, czy wyjście wymaga potwierdzenia (US7.4).
 *
 * @property place miejsce z pomiaru
 * @property floor piętro; poza budynkiem zawsze `null` (BR-G4)
 * @property barrierType typ bariery
 * @property severity uciążliwość
 * @property photoIds identyfikatory zrobionych zdjęć (BR-7)
 * @property description opis wpisany przez Zbierającego
 * @property replacesSubmissionId zgłoszenie przyjęte, które zapis zastąpi nowym (BR-18)
 */
data class ReportForm(
    val place: ReportPlace,
    val floor: ReportFloor? = null,
    val barrierType: BarrierType? = null,
    val severity: Severity? = null,
    val photoIds: List<String> = emptyList(),
    val description: String = "",
    val replacesSubmissionId: String? = null,
) {
    init {
        require(floor == null || place.buildingSymbol != null) { "Poza budynkiem formularz nie ma piętra (BR-G4)" }
    }
}

/** Braki, które blokują „Zapisz” (US7.1–US7.3, BR-7). */
fun ReportForm.errors(): List<ReportFormError> =
    buildList {
        if (place.buildingSymbol != null && floor == null) add(ReportFormError.MISSING_FLOOR)
        if (barrierType == null) add(ReportFormError.MISSING_BARRIER_TYPE)
        if (severity == null) add(ReportFormError.MISSING_SEVERITY)
        if (photoIds.isEmpty()) add(ReportFormError.MISSING_PHOTO)
        if (barrierType?.isDescriptionRequired == true && description.isBlank()) {
            add(ReportFormError.MISSING_DESCRIPTION)
        }
    }

/** Sprawdza formularz i przelicza go na treść pomiaru precyzyjnego z kontraktu (BR-7). */
fun ReportForm.check(): ReportCheck {
    val errors = errors()
    val type = barrierType
    val chosen = severity
    return if (errors.isNotEmpty() || type == null || chosen == null) {
        ReportCheck.Incomplete(errors)
    } else {
        ReportCheck.Complete(content(type, chosen))
    }
}

private fun ReportForm.content(type: BarrierType, chosen: Severity): SubmissionContent =
    SubmissionContent(
        kind = SubmissionKind.PRECISE,
        location = place.location,
        photoIds = photoIds,
        accuracyMeters = place.accuracyMeters,
        isManualCorrection = place.isManualCorrection,
        buildingSymbol = place.buildingSymbol,
        floor = floor?.toSubmissionFloor(),
        barrierGroup = type.group,
        barrierType = type.code,
        severity = chosen.value,
        description = description.trim().ifEmpty { null },
        replacesSubmissionId = replacesSubmissionId,
    )

private fun ReportFloor.toSubmissionFloor(): SubmissionFloor =
    when (this) {
        is ReportFloor.Planned -> SubmissionFloor(floorId = floorId)
        is ReportFloor.Numbered -> SubmissionFloor(number = number, isMezzanine = isMezzanine)
    }

/**
 * Numer piętra z pola „Numer piętra” w budynku podstawowym: liczba całkowita, także ujemna (US7.2).
 *
 * @return numer albo `null`, gdy tekst nie jest liczbą całkowitą
 */
fun parseFloorNumber(text: String): Int? = text.trim().toIntOrNull()
