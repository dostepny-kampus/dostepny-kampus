/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.barriers

import io.github.dostepnykampus.core.model.BarrierType
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.Severity
import io.github.dostepnykampus.core.model.Submission
import io.github.dostepnykampus.core.model.SubmissionKind
import io.github.dostepnykampus.core.network.ApiResult
import java.time.Instant

/**
 * Bariera ze zgłoszenia przyjętego, pokazywana na mapie V1 (US1.3, BR-6).
 *
 * @property id identyfikator zgłoszenia
 * @property location punkt bariery
 * @property severity uciążliwość, która wyznacza kształt znaku (NFR-21)
 * @property type typ bariery; `null`, gdy zgłoszenie go nie ma albo kod jest spoza klasyfikacji
 * @property buildingSymbol budynek; `null` to „poza budynkiem” (BR-G2)
 * @property verifiedAt chwila przyjęcia zgłoszenia (BR-17)
 */
data class AcceptedBarrier(
    val id: String,
    val location: GeoPoint,
    val severity: Severity,
    val type: BarrierType?,
    val buildingSymbol: String?,
    val verifiedAt: Instant?,
)

/** Bariery ze zgłoszeń przyjętych dla mapy (US1.3, BR-6). */
fun interface AcceptedBarriers {
    /**
     * Bariery z ostatniej odpowiedzi serwera. Pusta lista, gdy serwer ich nie podał (brak sieci, błąd serwera),
     * więc mapa działa dalej bez znaków barier (NFR-11).
     */
    suspend fun barriers(): List<AcceptedBarrier>
}

/**
 * [AcceptedBarriers] ze zgłoszeń przyjętych z serwera (`GET /submissions?status=ACCEPTED`).
 *
 * Każde wywołanie pyta serwer przez [accepted] i daje bariery tylko z tej odpowiedzi, więc zgłoszenie, które
 * przestało być przyjęte, znika. Zmiana oznaczenia wejścia, zgłoszenie „bariera usunięta” i zgłoszenie bez
 * uciążliwości nie są barierą do pokazania.
 *
 * @param accepted zgłoszenia przyjęte z serwera, np. `SubmissionsApi::accepted` bez budynku
 */
class ServerAcceptedBarriers(private val accepted: suspend () -> ApiResult<List<Submission>>) : AcceptedBarriers {
    override suspend fun barriers(): List<AcceptedBarrier> {
        val result = accepted()
        return if (result is ApiResult.Success) result.value.mapNotNull { it.toBarrier() } else emptyList()
    }
}

internal fun Submission.toBarrier(): AcceptedBarrier? {
    val severity = content.severity?.let(Severity::fromValue)
    val isShown = content.kind != SubmissionKind.ENTRANCE_MARKING && content.isBarrierRemoved != true
    return if (severity == null || !isShown) {
        null
    } else {
        AcceptedBarrier(
            id = id,
            location = content.location,
            severity = severity,
            type = content.barrierType?.let(BarrierType::fromCode),
            buildingSymbol = content.buildingSymbol,
            verifiedAt = verifiedAt,
        )
    }
}
