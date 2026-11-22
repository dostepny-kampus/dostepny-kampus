/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
import io.github.dostepnykampus.core.data.barrier.AcceptedBarriers

/**
 * Przyjęte bariery budynku [symbol] do wyboru wejścia zalecanego albo `null`, gdy telefon nie zsynchronizował barier.
 *
 * Wymagania: BR-25
 */
internal suspend fun AcceptedBarriers.barriersOf(symbol: String): List<AcceptedBarrier>? {
    val accepted = barriers()
    return syncedAt()?.let { accepted.filter { barrier -> barrier.buildingSymbol == symbol } }
}
