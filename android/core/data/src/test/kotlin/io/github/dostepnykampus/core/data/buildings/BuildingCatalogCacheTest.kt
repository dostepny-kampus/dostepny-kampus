/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.buildings

import io.github.dostepnykampus.core.database.BUILDING_CACHE_SINGLE_ROW
import io.github.dostepnykampus.core.database.BuildingCacheDao
import io.github.dostepnykampus.core.database.BuildingCacheEntity
import io.github.dostepnykampus.core.geo.parseGeoArea
import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.BuildingCatalog
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.network.ApiBuilding
import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.core.network.BuildingList
import io.github.dostepnykampus.core.network.DetailLevel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.Instant
import kotlin.coroutines.EmptyCoroutineContext

/** Kopia listy budynków w bazie: zapis przy pobraniu z serwera, czytanie bez sieci i data pobrania (NFR-10). */
class BuildingCatalogCacheTest {
    private val fake = FakeBuildingCacheDao()

    /** Wiersz: pobranie listy z serwera zapisuje kopię z momentem pobrania (NFR-10). */
    @Test
    fun nfr10_downloaded_saves_copy() {
        val repository = repository(remote = { listed("A-2") })
        runBlocking {
            repository.catalog()
            assertTrue(fake.entity?.run { listJson.contains("Serwer A-2") } == true)
            assertEquals(42L, fake.entity?.syncedAt)
            assertEquals(Instant.ofEpochMilli(42L), repository.syncedAt())
        }
    }

    /** Wiersz: bez sieci katalog przychodzi z kopii, z nazwami i konturami z serwera oraz datą pobrania (NFR-10). */
    @Test
    fun nfr10_offline_uses_copy() {
        val online = repository(remote = { listed("A-2", "N-1") })
        val offline = repository(remote = { OFFLINE })
        runBlocking {
            online.catalog()
            val restored = offline.catalog()
            assertEquals(SERVER_SOURCE, restored.attribution)
            assertEquals(listOf("A-2", "N-1"), restored.buildings.map { it.symbol })
            assertEquals(listOf("Serwer A-2", "Serwer N-1"), restored.buildings.map { it.name })
            assertEquals(listOf(outline(), outline()), restored.buildings.map { it.outline })
            assertEquals(Instant.ofEpochMilli(42L), offline.syncedAt())
        }
    }

    /** Wiersz: zepsuty dokument kopii to nie odpowiedź serwera — katalog zostaje z pliku, bez daty (NFR-10). */
    @Test
    fun nfr10_corrupt_copy_uses_asset() {
        fake.entity = BuildingCacheEntity(id = BUILDING_CACHE_SINGLE_ROW, listJson = "{", syncedAt = 7L)
        val repository = repository(remote = { OFFLINE })
        runBlocking {
            assertEquals(catalog(), repository.catalog())
            assertNull(repository.syncedAt())
        }
    }

    private fun catalog(): BuildingCatalog =
        runBlocking { BuildingCatalogRepository(EmptyCoroutineContext) { assetJson() }.catalog() }

    private fun repository(remote: (suspend () -> ApiResult<BuildingList>)?): BuildingCatalogRepository =
        BuildingCatalogRepository(
            ioContext = EmptyCoroutineContext,
            listBuildings = remote,
            readGeoJson = { assetJson() },
            buildingCache = fake,
            nowMillis = { 42L },
        )

    private fun listed(vararg symbols: String): ApiResult<BuildingList> {
        val buildings = symbols.map { symbol -> apiBuilding(symbol) }
        return ApiResult.Success(BuildingList(source = SERVER_SOURCE, buildings = buildings))
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
        val OFFLINE = ApiResult.NetworkError(IOException("brak sieci"))

        const val BUILDING_FEATURE =
            """{"type":"Feature","properties":{"kind":"building","symbols":["A-2"],"name":"Stary A-2",""" +
                """"osm_id":"way/A-2"},"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}}"""

        const val ENTRANCE_FEATURE =
            """{"type":"Feature","properties":{"kind":"entrance","symbols":["A-2"],"osm_id":"n/A-2",""" +
                """"entrance":"main"},"geometry":{"type":"Point","coordinates":[0,0]}}"""
    }
}

/** Kopia listy budynków w pamięci testu: jeden wiersz, więc test JVM obejdzie się bez Room i Robolectric. */
private class FakeBuildingCacheDao : BuildingCacheDao {
    var entity: BuildingCacheEntity? = null

    override suspend fun get(): BuildingCacheEntity? = entity

    override suspend fun save(entity: BuildingCacheEntity) {
        this.entity = entity
    }
}
