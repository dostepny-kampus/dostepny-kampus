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
import io.github.dostepnykampus.core.model.SubmissionFloor
import io.github.dostepnykampus.core.model.SubmissionKind
import io.github.dostepnykampus.core.network.ApiResult
import java.time.Instant

/**
 * Bariera ze zgłoszenia przyjętego, pokazywana na mapie V1.
 *
 * Wymagania: US1.3, BR-6, NFR-21, BR-G2, BR-17, US1.5
 *
 * @property id identyfikator zgłoszenia
 * @property location punkt bariery
 * @property severity uciążliwość, która wyznacza kształt znaku
 * @property type typ bariery; `null`, gdy zgłoszenie go nie ma albo kod jest spoza klasyfikacji
 * @property buildingSymbol budynek; `null` to „poza budynkiem”
 * @property verifiedAt chwila przyjęcia zgłoszenia
 * @property entranceId wejście, przy którym jest bariera; `null`, gdy zgłoszenie go nie wskazuje
 * @property floor piętro bariery; `null`, gdy zgłoszenie go nie wskazuje
 * @property photoIds zdjęcia zgłoszenia do arkusza bariery
 */
data class AcceptedBarrier(
    val id: String,
    val location: GeoPoint,
    val severity: Severity,
    val type: BarrierType?,
    val buildingSymbol: String?,
    val verifiedAt: Instant?,
    val entranceId: String? = null,
    val floor: SubmissionFloor? = null,
    val photoIds: List<String> = emptyList(),
)

/** Bariery ze zgłoszeń przyjętych dla mapy. Wymagania: US1.3, BR-6 */
fun interface AcceptedBarriers {
    /**
     * Bariery z ostatniej odpowiedzi serwera, a gdy serwer ich nie podał (brak sieci, błąd serwera), z ostatniej
     * kopii. Pusta lista, gdy nie ma ani odpowiedzi, ani kopii, więc mapa działa dalej bez znaków barier.
     *
     * Wymagania: NFR-10, NFR-11
     */
    suspend fun barriers(): List<AcceptedBarrier>

    /**
     * Chwila pobrania z serwera barier z ostatniego wywołania [barriers]: odpowiedzi albo kopii;
     * `null`, gdy bariery nie są ani z serwera, ani z kopii.
     *
     * Wymagania: NFR-10
     */
    fun syncedAt(): Instant? = null
}

/**
 * [AcceptedBarriers] ze zgłoszeń przyjętych z serwera (`GET /submissions?status=ACCEPTED`).
 *
 * Każde wywołanie pyta serwer przez [accepted]. Udana odpowiedź daje bariery tylko z niej i zastępuje kopię w bazie
 * [copy], więc zgłoszenie, które przestało być przyjęte, znika także z kopii. Gdy serwer nie odpowie, bariery
 * przychodzą z kopii, a bez niej lista jest pusta. Zmiana oznaczenia wejścia, zgłoszenie „bariera
 * usunięta” i zgłoszenie bez uciążliwości nie są barierą do pokazania.
 *
 * Wymagania: NFR-10
 *
 * @param copy kopia ostatnich barier w bazie; `null` zostawia źródło bez kopii
 * @param accepted zgłoszenia przyjęte z serwera, np. `SubmissionsApi::accepted` bez budynku
 */
class ServerAcceptedBarriers(
    private val copy: BarrierListCopy? = null,
    private val accepted: suspend () -> ApiResult<List<Submission>>,
) : AcceptedBarriers {
    @Volatile private var synced: Instant? = null

    override suspend fun barriers(): List<AcceptedBarrier> {
        val result = accepted()
        return if (result is ApiResult.Success) {
            val shown = result.value.mapNotNull { it.toBarrier() }
            synced = copy?.save(shown) ?: Instant.now()
            shown
        } else {
            val restored = copy?.restoreSynced()
            synced = restored?.syncedAt
            restored?.barriers.orEmpty()
        }
    }

    override fun syncedAt(): Instant? = synced
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
            entranceId = content.entranceId,
            floor = content.floor,
            photoIds = content.photoIds,
        )
    }
}
