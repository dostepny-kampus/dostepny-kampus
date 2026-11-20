/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.path

import io.github.dostepnykampus.core.data.cache.FakeServerCopyDao
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.database.cache.ACCESSIBLE_PATHS_COPY
import io.github.dostepnykampus.core.database.cache.ServerCopyEntity
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.path.AccessiblePath
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.coroutines.EmptyCoroutineContext

/** Ścieżki dostosowane z serwera z kopią w bazie na czas bez sieci. Wymagania: US1.4, NFR-10, NFR-11 */
class AccessiblePathsTest {
    private val dao = FakeServerCopyDao()

    @Test
    fun savesServerPathsAsCopy() =
        runBlocking {
            val online = source { ServerReply.Ok(listOf(PATH)) }.paths()

            assertEquals(listOf(PATH), online, "ścieżki z odpowiedzi serwera")
            assertEquals(SYNCED, dao.rows[ACCESSIBLE_PATHS_COPY]?.syncedAt, "chwila pobrania zapisana z kopią")
        }

    @Test
    fun givesCopyWhenServerFails() =
        runBlocking {
            source { ServerReply.Ok(listOf(PATH)) }.paths()

            assertEquals(
                List(2) { listOf(PATH) },
                listOf(source { ServerReply.Offline }.paths(), source { ServerReply.Fault(code = 500) }.paths()),
                "bez sieci i przy błędzie serwera ścieżki z kopii",
            )
        }

    @Test
    fun givesEmptyWithoutCopy() =
        runBlocking {
            assertEquals(emptyList<AccessiblePath>(), source { ServerReply.Offline }.paths(), "brak kopii")
        }

    @Test
    fun givesEmptyForBrokenCopy() =
        runBlocking {
            dao.save(ServerCopyEntity(key = ACCESSIBLE_PATHS_COPY, documentJson = "[{\"id\":", syncedAt = SYNCED))

            assertEquals(emptyList<AccessiblePath>(), source { ServerReply.Offline }.paths(), "zepsuta kopia")
        }

    @Test
    fun replacesCopyWithEmptyReply() =
        runBlocking {
            source { ServerReply.Ok(listOf(PATH)) }.paths()
            source { ServerReply.Ok(emptyList()) }.paths()

            assertEquals(emptyList<AccessiblePath>(), source { ServerReply.Offline }.paths(), "pusta kopia")
        }

    private fun source(server: suspend () -> ServerReply<List<AccessiblePath>>) =
        ServerAccessiblePaths(copy = pathCopy(dao, EmptyCoroutineContext), server = server) { SYNCED }

    private companion object {
        const val SYNCED = 42L
        val PATH =
            AccessiblePath(
                id = "p-1",
                line =
                    listOf(
                        GeoPoint(longitude = 17.06, latitude = 51.107),
                        GeoPoint(longitude = 17.061, latitude = 51.108),
                    ),
            )
    }
}
