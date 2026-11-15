/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.barrier

import java.time.Instant

/**
 * [AcceptedBarriers] ze źródła [source], w którym każda bariera ze zdjęciem dostaje adres pierwszego zdjęcia
 * w [AcceptedBarrier.photoUrl]. Bariera bez zdjęć zostaje bez adresu. Chwila pobrania pochodzi ze źródła.
 *
 * Wymagania: US1.5, US2.2, BR-17
 *
 * @param source bariery z serwera albo z kopii
 * @param address adres zdjęcia o identyfikatorze `photoId` zgłoszenia `submissionId`
 */
internal class PhotoAddressBarriers(
    private val source: AcceptedBarriers,
    private val address: (submissionId: String, photoId: String) -> String,
) : AcceptedBarriers {
    override suspend fun barriers(): List<AcceptedBarrier> =
        source.barriers().map { barrier ->
            barrier.copy(photoUrl = barrier.photoIds.firstOrNull()?.let { address(barrier.id, it) })
        }

    override fun syncedAt(): Instant? = source.syncedAt()
}
