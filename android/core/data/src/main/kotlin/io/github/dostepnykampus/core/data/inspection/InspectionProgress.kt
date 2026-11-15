/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.inspection

import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.data.server.toReply
import io.github.dostepnykampus.core.model.inspection.BuildingProgress
import io.github.dostepnykampus.core.network.inspection.InspectionProgressApi

/**
 * Postęp inwentaryzacji liczony na serwerze i cofnięcie znacznika przez Koordynatora. Odczyt i cofnięcie wymagają
 * sieci; bez niej wynik to [ServerReply.Offline], a nic nie czeka w kolejce.
 *
 * Wymagania: US11.1, US11.3, BR-21
 */
public interface InspectionProgress {
    /** Postęp wszystkich budynków z aktywnymi znacznikami; 403 bez roli Koordynatora to [ServerReply.Fault]. */
    public suspend fun buildings(): ServerReply<List<BuildingProgress>>

    /**
     * Cofa znacznik dowolnego Zbierającego. Nieznany znacznik to [ServerReply.Missing].
     *
     * @param markerId identyfikator UUID cofanego znacznika
     */
    public suspend fun revoke(markerId: String): ServerReply<Unit>
}

/**
 * Postęp z klienta [api].
 *
 * @param api klient HTTP postępu
 */
internal fun inspectionProgressOf(api: InspectionProgressApi): InspectionProgress =
    object : InspectionProgress {
        override suspend fun buildings(): ServerReply<List<BuildingProgress>> = api.progress().toReply { it }

        override suspend fun revoke(markerId: String): ServerReply<Unit> = api.revoke(markerId).toReply { }
    }
