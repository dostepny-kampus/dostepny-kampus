/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.server

import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.Entrance
import io.github.dostepnykampus.core.model.Submission

/**
 * Odpowiedź serwera bez typów klienta HTTP.
 *
 * @param T treść odpowiedzi
 */
public sealed interface ServerReply<out T> {
    /**
     * Serwer odpowiedział treścią.
     *
     * @param T treść odpowiedzi
     * @property value treść odpowiedzi
     */
    public data class Ok<T>(val value: T) : ServerReply<T>

    /** Brak połączenia z serwerem. */
    public data object Offline : ServerReply<Nothing>

    /** Serwer nie zna szukanego zasobu. */
    public data object Missing : ServerReply<Nothing>

    /**
     * Serwer odpowiedział błędem.
     *
     * @property code kod HTTP odpowiedzi
     */
    public data class Fault(val code: Int) : ServerReply<Nothing>
}

/**
 * Lista budynków z serwera; każdy budynek jest podstawowy, bez planów pięter.
 *
 * @property source atrybucja konturów
 * @property buildings budynki w kolejności odpowiedzi
 */
public data class ServerBuildings(val source: String, val buildings: List<Building>)

/**
 * Karta budynku podstawowego z serwera.
 *
 * @property building budynek karty
 * @property entrances wejścia budynku
 */
public data class ServerCard(val building: Building, val entrances: List<Entrance>)

/** Wynik edycji zgłoszenia z wersją bazową na serwerze. Wymagania: US9.3, BR-10 */
public sealed interface ServerEdit {
    /**
     * Serwer zapisał edycję jako kolejną wersję.
     *
     * @property submission zgłoszenie po edycji
     */
    public data class Saved(val submission: Submission) : ServerEdit

    /**
     * Serwer ma inną wersję albo zgłoszenie nie pozwala na zmianę (409).
     *
     * @property server zgłoszenie w wersji z serwera
     */
    public data class Conflict(val server: Submission) : ServerEdit
}
