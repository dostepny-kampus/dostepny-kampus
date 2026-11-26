/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.barrier

import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.submission.Severity
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

/** Adres zdjęcia bariery przyjętej. Wymagania: US1.5, US2.2, BR-17 */
class PhotoAddressBarriersTest {
    @Test
    fun addsAddressOfFirstPhoto() =
        runBlocking {
            val source = AcceptedBarriers { listOf(barrier(id = "z1", photoIds = listOf("p1", "p2"))) }
            val barriers = PhotoAddressBarriers(source) { submission, photo -> "/$submission/$photo" }.barriers()

            assertEquals(listOf("/z1/p1"), barriers.map(AcceptedBarrier::photoUrl), "adres pierwszego zdjęcia")
        }

    @Test
    fun leavesNoAddressWithoutPhotos() =
        runBlocking {
            val source = AcceptedBarriers { listOf(barrier(id = "z1", photoIds = emptyList())) }
            val barriers = PhotoAddressBarriers(source) { submission, photo -> "/$submission/$photo" }.barriers()

            assertEquals(listOf(null), barriers.map(AcceptedBarrier::photoUrl), "bez zdjęć nie ma adresu")
        }

    @Test
    fun keepsSyncedAtOfSource() {
        val source =
            object : AcceptedBarriers {
                override suspend fun barriers(): List<AcceptedBarrier> = emptyList()

                override fun syncedAt(): Instant = SYNCED
            }

        assertEquals(SYNCED, PhotoAddressBarriers(source) { _, _ -> "" }.syncedAt(), "chwila ze źródła")
    }

    /** Wiersz: bariery znane źródłu bez pytania serwera dostają adres zdjęcia jak przy pobraniu. */
    @Test
    fun addsAddressToKnown() {
        val source =
            object : AcceptedBarriers {
                override suspend fun barriers(): List<AcceptedBarrier> = emptyList()

                override fun known(): List<AcceptedBarrier> = listOf(barrier(id = "z1", photoIds = listOf("p1")))
            }

        val known = PhotoAddressBarriers(source) { submission, photo -> "/$submission/$photo" }.known()

        assertEquals(listOf("/z1/p1"), known?.map(AcceptedBarrier::photoUrl), "adres zdjęcia znanej bariery")
    }

    private fun barrier(id: String, photoIds: List<String>) =
        AcceptedBarrier(
            id = id,
            location = GeoPoint(longitude = 17.06, latitude = 51.107),
            severity = Severity.BLOCKING,
            type = null,
            buildingSymbol = null,
            verifiedAt = null,
            photoIds = photoIds,
        )

    private companion object {
        val SYNCED: Instant = Instant.parse("2026-10-14T10:00:00Z")
    }
}
