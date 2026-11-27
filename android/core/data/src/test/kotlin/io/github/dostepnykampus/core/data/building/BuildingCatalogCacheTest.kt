/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.building

import io.github.dostepnykampus.core.data.cache.FakeServerCopyDao
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.database.cache.BUILDINGS_COPY
import io.github.dostepnykampus.core.database.cache.ServerCopyDao
import io.github.dostepnykampus.core.database.cache.ServerCopyEntity
import io.github.dostepnykampus.core.geo.parseGeoArea
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.model.building.Building
import io.github.dostepnykampus.core.model.building.BuildingCatalog
import io.github.dostepnykampus.core.network.building.ApiBuilding
import io.github.dostepnykampus.core.network.building.BuildingList
import io.github.dostepnykampus.core.network.building.DetailLevel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Kopia listy budynków w bazie: zapis przy pobraniu z serwera, czytanie bez sieci i data pobrania.
 *
 * Wymagania: NFR-10
 */
class BuildingCatalogCacheTest {
    private val fake = FakeServerCopyDao()

    /** Wiersz: pobranie listy z serwera zapisuje kopię z momentem pobrania. Wymagania: NFR-10 */
    @Test
    fun savesDownloadedCopy() {
        val repository = repository(remote = { listed("A-2") })
        runBlocking {
            repository.catalog()
            val saved = fake.rows[BUILDINGS_COPY]
            assertTrue(saved?.run { documentJson.contains("Serwer A-2") } == true, "lista budynków zapisana w kopii")
            assertEquals(42L, saved?.syncedAt, "czas zapisu kopii")
            assertEquals(Instant.ofEpochMilli(42L), repository.syncedAt(), "czas synchronizacji z repozytorium")
        }
    }

    /**
     * Wiersz: bez sieci katalog przychodzi z kopii, z nazwami i konturami z serwera oraz datą pobrania.
     *
     * Wymagania: NFR-10
     */
    @Test
    fun usesCopyOffline() {
        val online = repository(remote = { listed("A-2", "N-1") })
        val offline = repository(remote = { OFFLINE })
        runBlocking {
            online.catalog()
            val restored = offline.catalog()
            assertEquals(SERVER_SOURCE, restored.attribution, "źródło danych z kopii")
            assertEquals(listOf("A-2", "N-1"), restored.buildings.map { it.symbol }, "symbole budynków z kopii")
            assertEquals(
                listOf("Serwer A-2", "Serwer N-1"),
                restored.buildings.map { it.name },
                "nazwy budynków z kopii",
            )
            assertEquals(listOf(outline(), outline()), restored.buildings.map { it.outline }, "obrysy budynków z kopii")
            assertEquals(Instant.ofEpochMilli(42L), offline.syncedAt(), "czas synchronizacji z kopii")
        }
    }

    /**
     * Wiersz: zepsuty dokument kopii to nie odpowiedź serwera — katalog zostaje z pliku, bez daty.
     *
     * Wymagania: NFR-10
     */
    @Test
    fun usesAssetForCorruptCopy() {
        fake.rows[BUILDINGS_COPY] = ServerCopyEntity(key = BUILDINGS_COPY, documentJson = "{", syncedAt = 7L)
        val repository = repository(remote = { OFFLINE })
        runBlocking {
            assertEquals(catalog(), repository.catalog(), "katalog wbudowany bez kopii")
            assertNull(repository.syncedAt(), "brak czasu synchronizacji bez kopii")
        }
    }

    /**
     * Wiersz: błąd zapisu kopii nie psuje katalogu — zwraca katalog z serwera i nie pyta go drugi raz.
     *
     * Wymagania: NFR-10, NFR-11
     */
    @Test
    fun keepsCatalogWhenSaveFails() {
        val calls = AtomicInteger()
        val repository =
            repository(
                remote = {
                    calls.incrementAndGet()
                    listed("A-2")
                },
                dao = copyDao(onSave = { error("disk full") }),
            )
        runBlocking {
            val first = repository.catalog()
            assertEquals(listOf("A-2"), first.buildings.map { it.symbol }, "katalog z serwera mimo błędu zapisu")
            assertEquals(SERVER_SOURCE, first.attribution, "źródło danych z serwera mimo błędu zapisu")
            assertEquals(Instant.ofEpochMilli(42L), repository.syncedAt(), "czas synchronizacji po błędzie zapisu")
            assertEquals(first, repository.catalog(), "drugie wywołanie oddaje zapamiętany katalog")
            assertEquals(1, calls.get(), "drugie wywołanie nie pyta serwera")
            val refreshed = repository.refresh()
            assertEquals(
                listOf("A-2"),
                refreshed.buildings.map { it.symbol },
                "odświeżenie zwraca katalog mimo błędu zapisu",
            )
            assertEquals(2, calls.get(), "odświeżenie pyta serwer raz")
        }
    }

    /**
     * Wiersz: błąd zapisu kopii nie przerywa strumienia aktualizacji katalogu.
     *
     * Wymagania: NFR-10, NFR-11
     */
    @Test
    fun emitsUpdatesWhenSaveFails() {
        val repository =
            repository(
                remote = { listed("A-2") },
                dao = copyDao(onSave = { error("disk full") }),
            )
        runBlocking {
            val emitted = repository.catalogUpdates().toList()
            assertEquals(2, emitted.size, "dwie emisje ze strumienia katalogu")
            assertEquals(listOf("A-2"), emitted.last().buildings.map { it.symbol }, "katalog z serwera w strumieniu")
        }
    }

    /**
     * Wiersz: błąd odczytu kopii bez sieci zostawia katalog z pliku bez czasu synchronizacji.
     *
     * Wymagania: NFR-10, NFR-11
     */
    @Test
    fun usesFileWhenRestoreFails() {
        val repository =
            repository(
                remote = { OFFLINE },
                dao = copyDao(onGet = { error("db broken") }),
            )
        runBlocking {
            assertEquals(catalog(), repository.catalog(), "katalog wbudowany przy błędzie odczytu kopii")
            assertNull(repository.syncedAt(), "brak czasu synchronizacji przy błędzie odczytu kopii")
        }
    }

    /**
     * Wiersz: anulowanie prowadzącego w trakcie zapisu kopii zostawia zapamiętany katalog z serwera.
     *
     * Wymagania: NFR-10, NFR-11
     */
    @Test
    fun keepsCatalogAfterSaveCancel() {
        val calls = AtomicInteger()
        val saving = CompletableDeferred<Unit>()
        val blockSave = CompletableDeferred<Unit>()
        val blockingDao =
            copyDao(
                onSave = {
                    if (calls.get() == 1) {
                        saving.complete(Unit)
                        blockSave.await()
                    }
                },
            )
        val repository =
            repository(
                remote = {
                    calls.incrementAndGet()
                    listed("A-2")
                },
                dao = blockingDao,
            )
        runBlocking {
            val leader = launch { repository.catalog() }
            saving.await()
            leader.cancelAndJoin()
            val afterCancel = repository.catalog()
            assertEquals(listOf("A-2"), afterCancel.buildings.map { it.symbol }, "katalog po anulowaniu zapisu")
            assertEquals(1, calls.get(), "kolejne wywołanie nie pyta serwera drugi raz")
        }
    }

    /**
     * Wiersz: anulowanie korutyny w czasie zapisu albo odczytu kopii wychodzi jako `CancellationException`.
     *
     * Wymagania: NFR-10, NFR-11
     */
    @Test
    fun rethrowsCopyCancellation() {
        val saving = CompletableDeferred<Unit>()
        val block = CompletableDeferred<Unit>()
        val blockingOnline =
            repository(
                remote = { listed("A-2") },
                dao =
                    copyDao(
                        onSave = {
                            saving.complete(Unit)
                            block.await()
                        },
                    ),
            )
        val throwingOnline =
            repository(
                remote = { listed("A-2") },
                dao = copyDao(onSave = { throw CancellationException("cancelled save") }),
            )
        val throwingOffline =
            repository(
                remote = { OFFLINE },
                dao = copyDao(onGet = { throw CancellationException("cancelled restore") }),
            )
        var isCompleted = false
        runBlocking {
            val saveJob =
                launch {
                    blockingOnline.catalog()
                    isCompleted = true
                }
            saving.await()
            saveJob.cancelAndJoin()
            assertFalse(isCompleted, "anulowanie podczas zapisu kopii przerywa catalog()")
            assertTrue(saveJob.isCancelled, "zadanie po anulowaniu zapisu ma stan anulowany")
        }
        assertThrows(
            CancellationException::class.java,
            { runBlocking { throwingOnline.catalog() } },
            "CancellationException podczas zapisu kopii wychodzi z catalog()",
        )
        assertThrows(
            CancellationException::class.java,
            { runBlocking { throwingOffline.catalog() } },
            "CancellationException podczas odczytu kopii wychodzi z catalog()",
        )
    }

    private fun copyDao(
        onGet: suspend () -> ServerCopyEntity? = { null },
        onSave: suspend () -> Unit = {},
    ): ServerCopyDao =
        object : ServerCopyDao {
            override suspend fun get(key: String): ServerCopyEntity? = onGet()

            override suspend fun save(entity: ServerCopyEntity) = onSave()

            override suspend fun delete(key: String) = Unit
        }

    private fun catalog(): BuildingCatalog =
        runBlocking { BuildingCatalogRepository(EmptyCoroutineContext) { assetJson() }.catalog() }

    private fun repository(
        remote: (suspend () -> ServerReply<BuildingList>)?,
        dao: ServerCopyDao = fake,
    ): BuildingCatalogRepository =
        BuildingCatalogRepository(
            ioContext = EmptyCoroutineContext,
            listBuildings = remote,
            readGeoJson = { assetJson() },
            serverCopy = dao,
            nowMillis = { 42L },
        )

    private fun listed(vararg symbols: String): ServerReply<BuildingList> {
        val buildings = symbols.map { symbol -> apiBuilding(symbol) }
        return ServerReply.Ok(BuildingList(source = SERVER_SOURCE, buildings = buildings))
    }

    private fun apiBuilding(symbol: String): ApiBuilding =
        ApiBuilding(
            building = Building(symbol = symbol, name = "Serwer $symbol", outline = outline()),
            detailLevel = DetailLevel.BASIC,
        )

    private fun outline(): GeoPolygon =
        parseGeoArea("""{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}""") as GeoPolygon

    private fun assetJson(): String =
        """{"type":"FeatureCollection","attribution":"$ASSET_SOURCE","features":[$BUILDING_FEATURE,$ENTRANCE_FEATURE]}"""

    private companion object {
        const val SERVER_SOURCE = "© serwer"
        const val ASSET_SOURCE = "© OpenStreetMap contributors"
        val OFFLINE = ServerReply.Offline

        const val BUILDING_FEATURE =
            """{"type":"Feature","properties":{"kind":"building","symbols":["A-2"],"display_name":"Stary A-2",""" +
                """"osm_id":"way/A-2"},"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}}"""

        const val ENTRANCE_FEATURE =
            """{"type":"Feature","properties":{"kind":"entrance","symbols":["A-2"],"osm_id":"n/A-2",""" +
                """"entrance":"main"},"geometry":{"type":"Point","coordinates":[0,0]}}"""
    }
}
