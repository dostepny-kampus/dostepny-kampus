/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.building

import android.content.res.AssetManager
import io.github.dostepnykampus.core.data.server.ServerBuildings
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.data.server.map
import io.github.dostepnykampus.core.database.cache.BuildingCacheDao
import io.github.dostepnykampus.core.geo.building.parseBuildingCatalog
import io.github.dostepnykampus.core.model.building.BuildingCatalog
import io.github.dostepnykampus.core.network.building.ApiBuilding
import io.github.dostepnykampus.core.network.building.BuildingList
import io.github.dostepnykampus.core.network.building.DetailLevel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

private typealias Running = CompletableDeferred<Result<BuildingCatalog>>

/** Plik konturów OSM w zasobach aplikacji; przy budowie kopiuje go z `data/buildings` zadanie `copyBuildingsAsset`. */
public const val BUILDINGS_ASSET: String = "pwr-buildings.geojson"

/**
 * Katalog budynków dla mapy. Z [listBuildings] kontury, nazwy i atrybucja są z serwera, a wejścia z pliku konturów,
 * tylko dla budynków podanych przez serwer. Bez niego, przy błędzie albo pustej liście katalog jest z pliku, chyba że
 * wcześniej pobrano katalog z serwera albo jego kopię w [buildingCache]. Budynki są sortowane naturalnie po symbolu
 * (A-2 przed A-10), bez wejść `emergency`, `exit` i `garage`. Trwające pobranie jest wspólne dla [catalogUpdates],
 * [catalog] i [refresh]; [catalogUpdates] oddaje najpierw katalog bez czekania, potem katalog z serwera. Błąd
 * prowadzącego dostają też czekający, bez ponownego odczytu, a jego anulowanie każe im pytać od nowa.
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
    private var inflight: Running? = null
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
     * Katalog, który telefon już ma, bez czekania: pobrany z serwera albo z pliku; `null` przed pierwszym odczytem
     * przez [catalog], [catalogUpdates] albo [refresh].
     */
    public fun current(): BuildingCatalog? = cached ?: downloaded.value?.catalog ?: bundled

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
        val mine = CompletableDeferred<Result<BuildingCatalog>>()
        val running =
            synchronized(lock) {
                val current = inflight
                if (current == null) inflight = mine
                current
            }
        return if (running == null) lead(mine, isFresh) else follow(running, isFresh)
    }

    private suspend fun follow(running: Running, isFresh: Boolean): BuildingCatalog {
        running.join()
        if (!running.isCancelled) return running.await().getOrThrow()
        synchronized(lock) { if (inflight === running) inflight = null }
        return fetched(isFresh)
    }

    private suspend fun lead(mine: Running, isFresh: Boolean): BuildingCatalog =
        try {
            supervisorScope {
                val work =
                    async(start = CoroutineStart.UNDISPATCHED) {
                        load(isFresh).also { mine.complete(Result.success(it)) }
                    }
                work.invokeOnCompletion { cause ->
                    when {
                        cause is CancellationException -> mine.cancel()
                        cause != null -> mine.complete(Result.failure(cause))
                    }
                }
                work.await()
            }
        } finally {
            synchronized(lock) { inflight = null }
            mine.cancel()
        }

    private suspend fun load(isFresh: Boolean): BuildingCatalog {
        val started = System.nanoTime()
        val file = readBundled(isFresh)
        val server = nonEmptyServerList()
        var source = if (server != null) "serwer" else "poprzedni odczyt"
        if (server != null) {
            val synced = SyncedBuildingList(list = server, syncedAtMillis = nowMillis())
            downloaded.value = Downloaded(file.withServerList(server), synced.syncedAtMillis)
            copy?.save(synced)
        } else if (downloaded.value == null) {
            val restored = copy?.restore()
            downloaded.value = restored?.let { Downloaded(file.withServerList(it.list), it.syncedAtMillis) }
            source = if (restored != null) "kopia z bazy" else "plik"
        }
        val result = downloaded.value?.catalog ?: file
        Timber.i("Katalog budynków: %s, %d budynków, %d ms", source, result.buildings.size, elapsedMillis(started))
        cached = result
        return result
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

internal fun elapsedMillis(startedNanos: Long): Long = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos)

private fun basicList(list: suspend () -> ServerReply<ServerBuildings>): suspend () -> ServerReply<BuildingList> =
    { list().map { found -> BuildingList(found.source, found.buildings.map { ApiBuilding(it, DetailLevel.BASIC) }) } }
