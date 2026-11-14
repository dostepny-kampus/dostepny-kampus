/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.buildings

import android.content.res.AssetManager
import io.github.dostepnykampus.core.data.server.ServerBuildings
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.data.server.map
import io.github.dostepnykampus.core.database.BuildingCacheDao
import io.github.dostepnykampus.core.geo.parseBuildingCatalog
import io.github.dostepnykampus.core.model.BuildingCatalog
import io.github.dostepnykampus.core.network.ApiBuilding
import io.github.dostepnykampus.core.network.BuildingList
import io.github.dostepnykampus.core.network.DetailLevel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import kotlin.coroutines.CoroutineContext

/** Plik konturów OSM w zasobach aplikacji; przy budowie kopiuje go z `data/buildings` zadanie `copyBuildingsAsset`. */
public const val BUILDINGS_ASSET: String = "pwr-buildings.geojson"

/**
 * Katalog budynków dla mapy. Z [listBuildings] kontury, nazwy i atrybucja są z serwera, a wejścia z pliku konturów,
 * tylko dla budynków podanych przez serwer. Bez niego, przy błędzie albo pustej liście katalog jest z pliku, chyba że
 * wcześniej pobrano katalog z serwera albo jego kopię w [buildingCache]. Budynki są sortowane naturalnie po symbolu
 * (A-2 przed A-10), bez wejść `emergency`, `exit` i `garage`. Trwające pobranie jest wspólne dla [catalogUpdates],
 * [catalog] i [refresh]; [catalogUpdates] oddaje najpierw katalog bez czekania, potem katalog z serwera. Anulowanie
 * prowadzącego pobranie każe pozostałym pytać od nowa. [syncedAt] to chwila pobrania z serwera, którą widzi katalog.
 *
 * Wymagania: US1, US1.2, US17.3, BR-25, NFR-11, NFR-10
 *
 * @param buildingCache kopia listy budynków w bazie; `null` zostawia katalog bez kopii
 * @param ioContext kontekst odczytu pliku i parsowania, np. dyspozytor wejścia-wyjścia
 * @param listBuildings lista z serwera; `null` zostawia katalog wyłącznie z pliku
 * @param nowMillis zegar chwili pobrania listy z serwera
 * @param readGeoJson odczyt treści pliku konturów
 */
public class BuildingCatalogRepository internal constructor(
    private val buildingCache: BuildingCacheDao?,
    private val ioContext: CoroutineContext,
    private val listBuildings: (suspend () -> ServerReply<BuildingList>)? = null,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val readGeoJson: () -> String,
) {
    private val lock = Any()
    private var inflight: CompletableDeferred<BuildingCatalog>? = null
    private val downloaded = MutableStateFlow<Downloaded?>(null)
    private val copy = buildingCache?.let { dao -> BuildingListCopy(dao, ioContext) }

    @Volatile private var cached: BuildingCatalog? = null

    @Volatile private var bundled: BuildingCatalog? = null

    /**
     * Katalog bez kopii listy budynków w bazie.
     *
     * @param ioContext kontekst odczytu pliku i parsowania, np. dyspozytor wejścia-wyjścia
     * @param listBuildings lista budynków podstawowych z serwera; `null` zostawia katalog wyłącznie z pliku
     * @param nowMillis zegar chwili pobrania listy z serwera
     * @param readGeoJson odczyt treści pliku konturów
     */
    public constructor(
        ioContext: CoroutineContext,
        listBuildings: (suspend () -> ServerReply<ServerBuildings>)? = null,
        nowMillis: () -> Long = System::currentTimeMillis,
        readGeoJson: () -> String,
    ) : this(null, ioContext, listBuildings?.let(::basicList), nowMillis, readGeoJson)

    /**
     * Zwraca katalog budynków; gdy trwa pobranie z serwera, czeka na jego wynik.
     *
     * @throws IllegalArgumentException gdy plik konturów ma zły format
     */
    public suspend fun catalog(): BuildingCatalog = fetched(isFresh = false)

    /**
     * Strumień katalogu dla mapy: najpierw katalog z pliku (albo już pobrany z serwera), bez czekania na pobranie,
     * potem katalog z serwera, gdy ten odpowie niepustą listą, także przyjęty przez [catalog] lub [refresh]. Błąd,
     * brak sieci i pusta lista nie dają drugiej emisji. Strumień kończy się po katalogu z serwera; bez niego czeka na
     * katalog przyjęty później (np. przez [refresh]) i kończy go anulowanie zbierającego.
     *
     * @throws IllegalArgumentException gdy plik konturów ma zły format
     */
    public fun catalogUpdates(): Flow<BuildingCatalog> =
        channelFlow {
            val first = downloaded.value?.catalog ?: readBundled(isFresh = false)
            send(first)
            launch { fetched(isFresh = false) }
            send(downloaded.filterNotNull().map { server -> server.catalog }.first { server -> server !== first })
        }

    /**
     * Pyta serwer i czyta plik od nowa, zapamiętuje wynik i zwraca katalog dla „Spróbuj ponownie”, gdy budynku
     * nie ma w telefonie. Gdy pobranie już trwa, czeka na jego wynik. Gdy serwer nie odpowie, zostaje
     * ostatni katalog z serwera, a bez niego kopia z bazy [buildingCache], a bez kopii plik.
     *
     * Wymagania: NFR-10
     *
     * @throws IllegalArgumentException gdy plik konturów ma zły format
     */
    public suspend fun refresh(): BuildingCatalog = fetched(isFresh = true)

    /**
     * Chwila ostatniego, udanego pobrania listy budynków z serwera, które widzi katalog; `null`, gdy katalog
     * jest z pliku.
     *
     * Wymagania: NFR-10
     */
    public fun syncedAt(): Instant? = downloaded.value?.run { Instant.ofEpochMilli(syncedAtMillis) }

    private suspend fun fetched(isFresh: Boolean): BuildingCatalog {
        val ready = if (isFresh) null else cached
        if (ready != null) return ready
        val mine = CompletableDeferred<BuildingCatalog>()
        val running =
            synchronized(lock) {
                val current = inflight
                if (current == null) inflight = mine
                current
            }
        return if (running == null) lead(mine, isFresh) else follow(running, isFresh)
    }

    private suspend fun follow(running: CompletableDeferred<BuildingCatalog>, isFresh: Boolean): BuildingCatalog {
        running.join()
        return if (running.isCancelled) fetched(isFresh) else running.await()
    }

    private suspend fun lead(mine: CompletableDeferred<BuildingCatalog>, isFresh: Boolean): BuildingCatalog {
        try {
            val file = readBundled(isFresh)
            val server = nonEmptyServerList()
            if (server != null) {
                val synced = SyncedBuildingList(list = server, syncedAtMillis = nowMillis())
                downloaded.value = Downloaded(file.withServerList(server), synced.syncedAtMillis)
                copy?.save(synced)
            } else if (downloaded.value == null) {
                val restored = copy?.restore()
                downloaded.value = restored?.let { Downloaded(file.withServerList(it.list), it.syncedAtMillis) }
            }
            val result = downloaded.value?.catalog ?: file
            cached = result
            mine.complete(result)
            return result
        } finally {
            synchronized(lock) { inflight = null }
            mine.cancel()
        }
    }

    private suspend fun readBundled(isFresh: Boolean): BuildingCatalog =
        (if (isFresh) null else bundled)
            ?: withContext(ioContext) { parseBuildingCatalog(readGeoJson()).forVisitors() }.also { bundled = it }

    private suspend fun nonEmptyServerList(): BuildingList? {
        val result = listBuildings?.invoke()
        return if (result is ServerReply.Ok && result.value.buildings.isNotEmpty()) result.value else null
    }

    /** Katalog z serwera (albo z jego kopii w bazie) razem z chwilą pobrania listy: jedna wartość stanu. */
    private data class Downloaded(val catalog: BuildingCatalog, val syncedAtMillis: Long)
}

/** [BuildingCatalogRepository] na pliku [BUILDINGS_ASSET]; [listBuildings] to lista z serwera, a bez niej sam plik. */
internal fun assetRepository(
    assets: AssetManager,
    ioContext: CoroutineContext = Dispatchers.IO,
    listBuildings: (suspend () -> ServerReply<BuildingList>)? = null,
    buildingCache: BuildingCacheDao? = null,
): BuildingCatalogRepository =
    BuildingCatalogRepository(
        ioContext = ioContext,
        listBuildings = listBuildings,
        readGeoJson = { assets.open(BUILDINGS_ASSET).bufferedReader().use { it.readText() } },
        buildingCache = buildingCache,
    )

private fun basicList(list: suspend () -> ServerReply<ServerBuildings>): suspend () -> ServerReply<BuildingList> =
    { list().map { found -> BuildingList(found.source, found.buildings.map { ApiBuilding(it, DetailLevel.BASIC) }) } }
