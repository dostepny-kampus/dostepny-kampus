/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.path

import io.github.dostepnykampus.core.data.cache.ServerCopy
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.model.path.AccessiblePath
import java.time.Instant

/** Ścieżki dostosowane kampusu dla mapy. Wymagania: US1.4 */
public fun interface AccessiblePaths {
    /**
     * Odcinki z odpowiedzi serwera, a gdy serwer ich nie podał (brak sieci, błąd serwera), z ostatniej kopii.
     * Pusta lista, gdy nie ma ani odpowiedzi, ani kopii, więc mapa działa dalej bez ścieżek.
     *
     * Wymagania: NFR-10, NFR-11
     */
    public suspend fun paths(): List<AccessiblePath>
}

/**
 * [AccessiblePaths] z serwera (`GET /accessible-paths`). Udana odpowiedź zastępuje kopię [copy], także pusta.
 *
 * @param copy kopia ostatnich ścieżek w bazie
 * @param server ścieżki z serwera
 * @param nowMillis zegar chwili pobrania z serwera
 */
internal class ServerAccessiblePaths(
    private val copy: ServerCopy<List<AccessiblePath>>,
    private val server: suspend () -> ServerReply<List<AccessiblePath>>,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : AccessiblePaths {
    override suspend fun paths(): List<AccessiblePath> {
        val reply = server()
        return if (reply is ServerReply.Ok) {
            copy.save(reply.value, Instant.ofEpochMilli(nowMillis()))
            reply.value
        } else {
            copy.restore()?.value.orEmpty()
        }
    }
}
