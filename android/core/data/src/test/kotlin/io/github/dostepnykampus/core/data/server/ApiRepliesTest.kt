/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.server

import io.github.dostepnykampus.core.network.ApiResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.IOException

/** Wynik klienta HTTP staje się odpowiedzią serwera bez typów sieci. Wymagania: NFR-10 */
class ApiRepliesTest {
    @Test
    fun nfr10_success_is_ok_with_content() {
        assertEquals(ServerReply.Ok(TEXT.length), reply(ApiResult.Success(TEXT)))
    }

    @Test
    fun nfr10_not_found_is_missing() {
        assertEquals(ServerReply.Missing, reply(ApiResult.NotFound))
    }

    @Test
    fun nfr10_network_error_is_offline() {
        assertEquals(ServerReply.Offline, reply(ApiResult.NetworkError(IOException("brak sieci"))))
    }

    @Test
    fun nfr10_server_error_is_fault_with_code() {
        assertEquals(ServerReply.Fault(SERVER_ERROR), reply(ApiResult.ServerError(SERVER_ERROR)))
    }

    private fun reply(result: ApiResult<String>): ServerReply<Int> = result.toReply { it.length }

    private companion object {
        const val TEXT = "karta"
        const val SERVER_ERROR = 503
    }
}
