/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.dostepnykampus.core.data.inspection.FloorMarkers
import io.github.dostepnykampus.core.data.inspection.InspectionProgress
import io.github.dostepnykampus.core.data.inspection.MarkerSync
import io.github.dostepnykampus.core.data.inspection.floorMarkersOf
import io.github.dostepnykampus.core.data.inspection.inspectionProgressOf
import io.github.dostepnykampus.core.data.submission.sync.SubmissionSyncScheduler
import io.github.dostepnykampus.core.database.inspection.PendingMarkerChangeDao
import io.github.dostepnykampus.core.network.auth.AccessTokens
import io.github.dostepnykampus.core.network.inspection.InspectionProgressApi
import io.github.dostepnykampus.core.network.inspection.MarkerChangesApi

/** Wiązania Hilt znaczników piętra obejrzanego i postępu inwentaryzacji. */
@Module
@InstallIn(SingletonComponent::class)
internal object InspectionDataModule {
    /** Znaczniki piętra obejrzanego z kolejką w bazie telefonu; zapis planuje wysyłkę. Wymagania: US15.3, BR-9 */
    @Provides
    fun floorMarkers(queue: PendingMarkerChangeDao, sync: SubmissionSyncScheduler, tokens: AccessTokens): FloorMarkers =
        floorMarkersOf(queue = queue, subjects = tokens.subjects, onSaved = sync::scheduleAfterSave)

    /** Wysyłka kolejki znaczników przez `POST /sync/push`. Wymagania: US15.3, BR-9, NFR-6 */
    @Provides
    fun markerSync(queue: PendingMarkerChangeDao, api: MarkerChangesApi, tokens: AccessTokens): MarkerSync =
        MarkerSync(queue, tokens::subject) { owner, changes -> api.push(changes, owner) }

    /** Postęp inwentaryzacji z serwera. Wymagania: US11.1, US11.3, BR-21 */
    @Provides
    fun inspectionProgress(api: InspectionProgressApi): InspectionProgress = inspectionProgressOf(api)
}
