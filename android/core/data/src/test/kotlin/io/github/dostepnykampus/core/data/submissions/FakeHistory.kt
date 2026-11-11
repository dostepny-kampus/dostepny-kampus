/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submissions

import io.github.dostepnykampus.core.database.OwnSubmissionDao
import io.github.dostepnykampus.core.database.OwnSubmissionEntity
import io.github.dostepnykampus.core.database.OwnSubmissionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Historia własnych zgłoszeń w pamięci, jak w testach Room. */
internal class FakeHistory : OwnSubmissionDao {
    val rows = mutableMapOf<String, OwnSubmissionEntity>()

    override fun observe(): Flow<List<OwnSubmissionEntity>> =
        flow { emit(rows.values.sortedByDescending { it.savedAtMillis }) }

    override suspend fun save(submission: OwnSubmissionEntity) {
        rows[submission.id] = submission
    }

    override suspend fun find(id: String): OwnSubmissionEntity? = rows[id]

    override suspend fun saveVersion(id: String, version: Int) {
        rows.computeIfPresent(id) { _, row -> row.copy(serverVersion = version) }
    }

    override suspend fun markSent(id: String) {
        rows.computeIfPresent(id) { _, row -> row.copy(state = OwnSubmissionState.SENT, refusalCode = null) }
    }

    override suspend fun markRefused(id: String, code: Int?) {
        rows.computeIfPresent(id) { _, row -> row.copy(state = OwnSubmissionState.REFUSED, refusalCode = code) }
    }

    override suspend fun saveServerStatus(id: String, status: String, reason: String?, version: Int) {
        rows.computeIfPresent(id) { _, row ->
            row.copy(serverStatus = status, rejectionReason = reason, serverVersion = version)
        }
    }
}
