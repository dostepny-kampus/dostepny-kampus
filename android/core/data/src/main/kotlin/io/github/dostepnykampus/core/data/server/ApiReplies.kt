/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.server

import io.github.dostepnykampus.core.network.ApiResult

/**
 * Odpowiedź serwera z wyniku klienta HTTP: treść przez [content], brak zasobu jako [ServerReply.Missing], brak
 * połączenia jako [ServerReply.Offline], błąd serwera jako [ServerReply.Fault] z kodem HTTP.
 *
 * Wymagania: NFR-10
 */
internal fun <T, R> ApiResult<T>.toReply(content: (T) -> R): ServerReply<R> =
    when (this) {
        is ApiResult.Success -> ServerReply.Ok(content(value))
        ApiResult.NotFound -> ServerReply.Missing
        is ApiResult.NetworkError -> ServerReply.Offline
        is ApiResult.ServerError -> ServerReply.Fault(code)
    }
