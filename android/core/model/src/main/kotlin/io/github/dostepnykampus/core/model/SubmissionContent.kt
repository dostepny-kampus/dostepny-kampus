/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.model

/**
 * Treść zgłoszenia ustalana przez składającego (`SubmissionContent` z kontraktu API).
 *
 * @property kind rodzaj zgłoszenia
 * @property location miejsce bariery po pomiarze precyzyjnym albo bieżąca pozycja
 * @property photoIds identyfikatory zdjęć nadane w telefonie; co najmniej jedno (BR-7, BR-8)
 * @property accuracyMeters dokładność pozycji w metrach (US6.1)
 * @property isManualCorrection oznaczenie „korekta ręczna” (US6.3, US6.4); `null` to pole nieobecne,
 *   które kontrakt API traktuje jak `false`
 * @property buildingSymbol budynek; `null` to „poza budynkiem” (BR-G2). W zgłoszeniu szybkim ustala go serwer (BR-14)
 * @property floor piętro zgłoszenia
 * @property barrierGroup grupa barier w zgłoszeniu szybkim (US4.3)
 * @property barrierType typ bariery TB-1…TB-29 (BR-7)
 * @property severity uciążliwość 1–3; 3 to blokada
 * @property description opis; obowiązkowy przy TB-29 „inna bariera” (US7.3)
 * @property replacesSubmissionId zgłoszenie przyjęte, które ta zmiana zastąpi (BR-18)
 * @property isBarrierRemoved oznaczenie „bariera usunięta” (US8.4, BR-19); `null` to pole nieobecne,
 *   które kontrakt API traktuje jak `false`
 * @property entranceId wejście w zmianie oznaczenia wejścia (BR-24)
 * @property entranceMarking nowe oznaczenie wejścia: tylko dostępne albo niedostępne (US17.2)
 */
data class SubmissionContent(
    val kind: SubmissionKind,
    val location: GeoPoint,
    val photoIds: List<String>,
    val accuracyMeters: Double? = null,
    val isManualCorrection: Boolean? = null,
    val buildingSymbol: String? = null,
    val floor: SubmissionFloor? = null,
    val barrierGroup: BarrierGroup? = null,
    val barrierType: String? = null,
    val severity: Int? = null,
    val description: String? = null,
    val replacesSubmissionId: String? = null,
    val isBarrierRemoved: Boolean? = null,
    val entranceId: String? = null,
    val entranceMarking: EntranceMarking? = null,
) {
    init {
        require(photoIds.isNotEmpty()) { "Zgłoszenie wymaga co najmniej jednego zdjęcia (BR-7, BR-8)" }
        require(severity == null || severity in SEVERITY) { "Uciążliwość spoza zakresu 1–3" }
        require(entranceMarking != EntranceMarking.NO_DATA) { "Zmiana oznaczenia wejścia nie może dać „brak danych”" }
    }

    private companion object {
        val SEVERITY = 1..3
    }
}

/**
 * Piętro zgłoszenia (`SubmissionFloor`, US7.2, BR-G1).
 *
 * @property floorId piętro z listy pięter budynku szczegółowego
 * @property number numer piętra w budynku podstawowym; 0 to parter
 * @property isMezzanine dopisek „półpiętro” w budynku podstawowym; `null` to pole nieobecne,
 *   które kontrakt API traktuje jak `false`
 */
data class SubmissionFloor(val floorId: String? = null, val number: Int? = null, val isMezzanine: Boolean? = null)

/** Rodzaj zgłoszenia (`kind` w `SubmissionContent`). */
enum class SubmissionKind {
    /** Pomiar precyzyjny Zbierającego (US7, BR-7). */
    PRECISE,

    /** Zgłoszenie szybkie zdjęciem i bieżącą pozycją (US4, BR-8). */
    QUICK,

    /** Zmiana oznaczenia wejścia (US17.2, BR-24). */
    ENTRANCE_MARKING,
}

/** Grupa barier (`BarrierGroup`) w zgłoszeniu szybkim (US4.3). */
enum class BarrierGroup {
    /** Wejście i drzwi. */
    ENTRANCE_AND_DOORS,

    /** Komunikacja pionowa: windy, schody, pochylnie. */
    VERTICAL_CIRCULATION,

    /** Komunikacja pozioma: korytarze, progi. */
    HORIZONTAL_CIRCULATION,

    /** Toalety. */
    TOILETS,

    /** Orientacja i informacja. */
    ORIENTATION_AND_INFORMATION,

    /** Inne. */
    OTHER,
}
