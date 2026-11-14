/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network.inspection

/**
 * Zmiana znacznika piętra obejrzanego czekająca w telefonie na wysyłkę.
 *
 * Wymagania: US15.3, BR-9
 */
public sealed interface MarkerChange {
    /** Identyfikator operacji nadany w telefonie; serwer rozpoznaje po nim powtórzoną wysyłkę. */
    public val operationId: String

    /**
     * Postawienie znacznika.
     *
     * @property operationId identyfikator operacji
     * @property markerId identyfikator nowego znacznika
     * @property buildingSymbol budynek znacznika
     * @property floorId piętro budynku szczegółowego; `null`, gdy znacznik dotyczy całego budynku podstawowego
     */
    public data class Mark(
        override val operationId: String,
        val markerId: String,
        val buildingSymbol: String,
        val floorId: String?,
    ) : MarkerChange

    /**
     * Cofnięcie znacznika.
     *
     * @property operationId identyfikator operacji
     * @property markerId cofany znacznik
     */
    public data class Revoke(override val operationId: String, val markerId: String) : MarkerChange
}

/**
 * Wynik jednej zmiany znacznika na serwerze.
 *
 * @property operationId identyfikator operacji z żądania
 * @property isApplied `true`, gdy serwer zapisał zmianę; `false`, gdy ją odrzucił, np. z braku roli
 */
public data class MarkerChangeResult(val operationId: String, val isApplied: Boolean)
