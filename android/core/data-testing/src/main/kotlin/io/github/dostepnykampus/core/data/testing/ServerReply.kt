/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.testing

import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.Entrance
import io.github.dostepnykampus.core.network.ApiResult
import java.io.IOException

/**
 * Odpowiedź serwera w teście modułu funkcji, bez typów klienta HTTP.
 *
 * @param T treść odpowiedzi
 */
sealed interface ServerReply<out T> {
    /**
     * Serwer odpowiedział treścią.
     *
     * @param T treść odpowiedzi
     * @property value treść odpowiedzi
     */
    data class Ok<T>(val value: T) : ServerReply<T>

    /** Brak połączenia z serwerem. */
    data object Offline : ServerReply<Nothing>

    /**
     * Serwer odpowiedział błędem.
     *
     * @property code kod HTTP odpowiedzi
     */
    data class Fault(val code: Int) : ServerReply<Nothing>
}

/**
 * Lista budynków z serwera; każdy budynek jest podstawowy, bez planów pięter.
 *
 * @property source atrybucja konturów
 * @property buildings budynki w kolejności odpowiedzi
 */
data class ServerBuildings(val source: String, val buildings: List<Building>)

/**
 * Karta budynku podstawowego z serwera.
 *
 * @property building budynek karty
 * @property entrances wejścia budynku
 */
data class ServerCard(val building: Building, val entrances: List<Entrance>)

internal fun <T, R> ServerReply<T>.toApiResult(content: (T) -> R): ApiResult<R> =
    when (this) {
        is ServerReply.Ok -> ApiResult.Success(content(value))
        ServerReply.Offline -> ApiResult.NetworkError(IOException("brak sieci"))
        is ServerReply.Fault -> ApiResult.ServerError(code)
    }
