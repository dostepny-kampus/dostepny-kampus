/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.room

import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.database.room.FloorPlanDao
import io.github.dostepnykampus.core.model.building.Floor
import io.github.dostepnykampus.core.model.building.FloorList
import io.github.dostepnykampus.core.model.room.RoomCategory
import io.github.dostepnykampus.core.model.room.RoomList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

/**
 * Kategoria i funkcja pomieszczenia z kopii planu, bez jego geometrii.
 *
 * Wymagania: US2.4, NFR-56
 *
 * @property category grupa funkcji pomieszczenia
 * @property function funkcja z danych PWr
 */
public data class RoomSummary(val category: RoomCategory, val function: String)

/**
 * Plany pięter budynków z kopią w telefonie, do rysowania planu bez sieci.
 *
 * Wymagania: US16.1, NFR-10, NFR-40
 */
public interface FloorPlans {
    /**
     * Pobiera piętra i pomieszczenia wszystkich pięter budynku [symbol] i zastępuje nimi jego kopię. Gdy pobranie
     * któregokolwiek piętra się nie uda, kopia zostaje w całości bez zmian.
     *
     * @param symbol symbol budynku
     * @return `true`, gdy kopia jest właśnie pobrana z serwera
     */
    public suspend fun download(symbol: String): Boolean

    /** Piętra budynku [symbol] z kopii od najniższego; `null`, gdy kopia nie ma jego pięter. */
    public suspend fun floors(symbol: String): FloorList?

    /** Pomieszczenia piętra [floor] budynku [symbol] z kopii; `null`, gdy kopia nie ma piętra albo jest zepsuta. */
    public suspend fun rooms(symbol: String, floor: String): RoomList?

    /**
     * Kategorie i funkcje pomieszczeń budynku [symbol] z kopii według identyfikatora piętra, bez geometrii;
     * piętro z zepsutą kopią jest pomijane.
     *
     * Wymagania: US2.4, NFR-56
     */
    public suspend fun roomFacts(symbol: String): Map<String, List<RoomSummary>> =
        floors(symbol)
            ?.floors
            .orEmpty()
            .mapNotNull { floor ->
                rooms(symbol, floor.id)?.let { list ->
                    floor.id to list.rooms.map { RoomSummary(category = it.category, function = it.function) }
                }
            }.toMap()
}

/**
 * [FloorPlans] z serwera (`listFloors`, `listFloorRooms`) z kopią w tabelach `floor_plans` i `floor_rooms`.
 * Pomieszczenia piętra odczytane raz zostają w pamięci do następnego pobrania planu budynku.
 *
 * @param dao tabele kopii planów
 * @param fetchFloors piętra budynku z serwera
 * @param fetchRooms pomieszczenia piętra budynku z serwera
 * @param ioContext kontekst zapisu i odczytu bazy
 * @param now chwila pobrania w milisekundach epoki
 */
internal class FloorPlanCopy(
    private val dao: FloorPlanDao,
    private val fetchFloors: suspend (String) -> ServerReply<FloorList>,
    private val fetchRooms: suspend (String, String) -> ServerReply<RoomList>,
    private val ioContext: CoroutineContext = Dispatchers.IO,
    private val now: () -> Long = System::currentTimeMillis,
) : FloorPlans {
    private val read = ConcurrentHashMap<Pair<String, String>, RoomList>()

    override suspend fun download(symbol: String): Boolean {
        val plans = fetchPlans(symbol) ?: return false
        val syncedAt = now()
        withContext(ioContext) {
            dao.replace(
                building = symbol,
                plans = plans.map { (floor, list) -> list.toPlanEntity(symbol, floor, syncedAt) },
                rooms =
                    plans.flatMap { (floor, list) ->
                        list.rooms.map { it.toEntity(building = symbol, floor = floor.id) }
                    },
            )
        }
        read.keys.removeAll { it.first == symbol }
        return true
    }

    private suspend fun fetchPlans(symbol: String): List<Pair<Floor, RoomList>>? =
        (fetchFloors(symbol) as? ServerReply.Ok)?.run {
            value.floors.map { floor ->
                floor to ((fetchRooms(symbol, floor.id) as? ServerReply.Ok)?.value ?: return null)
            }
        }

    override suspend fun floors(symbol: String): FloorList? =
        withContext(ioContext) { dao.floors(symbol) }.takeIf { it.isNotEmpty() }?.let { rows ->
            FloorList(
                source = rows.first().source,
                floors = rows.map { Floor(id = it.floor, label = it.label, order = it.order) },
            )
        }

    override suspend fun rooms(symbol: String, floor: String): RoomList? =
        read[symbol to floor] ?: withContext(ioContext) {
            dao.plan(building = symbol, floor = floor)?.toRoomList(dao.rooms(building = symbol, floor = floor))
        }?.also { read[symbol to floor] = it }

    override suspend fun roomFacts(symbol: String): Map<String, List<RoomSummary>> {
        val rows = withContext(ioContext) { dao.roomFacts(symbol) }
        return rows
            .groupBy { it.floor }
            .mapNotNull { (floor, floorRows) -> floorRows.toRoomSummaries()?.let { floor to it } }
            .toMap()
    }
}
