/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.model.inspection

import java.time.Instant

/**
 * Postęp inwentaryzacji jednego budynku (`BuildingProgress` z kontraktu API).
 *
 * Wymagania: US11.1, US11.3, BR-21
 *
 * @property buildingSymbol symbol budynku
 * @property coverage obejrzane piętra budynku szczegółowego albo znacznik budynku podstawowego
 * @property pendingSubmissions zgłoszenia wysłane i w weryfikacji
 * @property acceptedSubmissions zgłoszenia przyjęte, bez archiwalnych
 * @property entrancesTotal wszystkie wejścia budynku
 * @property entrancesCheckedOnSite wejścia ze źródłem oznaczenia „obchód”
 * @property markers aktywne znaczniki z autorem do cofnięcia przez Koordynatora
 */
public data class BuildingProgress(
    val buildingSymbol: String,
    val coverage: InspectionCoverage,
    val pendingSubmissions: Int,
    val acceptedSubmissions: Int,
    val entrancesTotal: Int,
    val entrancesCheckedOnSite: Int,
    val markers: List<ProgressMarker>,
)

/** Co zostało obejrzane w budynku. Wymagania: US11.1, BR-21 */
public sealed interface InspectionCoverage {
    /**
     * Budynek szczegółowy: piętra z danych PWr.
     *
     * @property floorsTotal liczba pięter
     * @property floorsInspected liczba pięter obejrzanych
     */
    public data class Floors(val floorsTotal: Int, val floorsInspected: Int) : InspectionCoverage

    /**
     * Budynek podstawowy: jeden znacznik dla całego budynku.
     *
     * @property isInspected czy znacznik „obejrzany” jest postawiony
     */
    public data class Whole(val isInspected: Boolean) : InspectionCoverage
}

/**
 * Znacznik piętra obejrzanego zapisany na serwerze (`InspectionMarker` z kontraktu API).
 *
 * Wymagania: US11.3, US15.2
 *
 * @property id identyfikator UUID znacznika
 * @property buildingSymbol budynek znacznika
 * @property place obejrzane piętro albo cały budynek
 * @property createdAt chwila odbioru znacznika przez serwer
 * @property isRevoked czy znacznik cofnięto
 * @property isMine czy znacznik postawiła osoba z tokenu
 */
public data class InspectionMarker(
    val id: String,
    val buildingSymbol: String,
    val place: MarkerPlace,
    val createdAt: Instant,
    val isRevoked: Boolean,
    val isMine: Boolean,
)

/**
 * Znacznik w postępie inwentaryzacji widziany przez Koordynatora (`ProgressMarker` z kontraktu API).
 *
 * Wymagania: US11.3, BR-5
 *
 * @property marker znacznik zapisany na serwerze
 * @property authorPseudonym pseudonim autora bez danych osobowych, np. „Autor 14”
 */
public data class ProgressMarker(val marker: InspectionMarker, val authorPseudonym: String)

/** Czego dotyczy znacznik. Wymagania: US15.1, BR-20 */
public sealed interface MarkerPlace {
    /**
     * Piętro budynku szczegółowego.
     *
     * @property floorId identyfikator piętra
     */
    public data class Floor(val floorId: String) : MarkerPlace

    /** Cały budynek podstawowy: znacznik „obejrzany”. */
    public data object WholeBuilding : MarkerPlace
}
