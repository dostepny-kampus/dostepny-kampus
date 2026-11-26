/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.review.queue

import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.submission.BarrierGroup
import io.github.dostepnykampus.core.model.submission.Submission
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import io.github.dostepnykampus.core.model.submission.SubmissionStatus
import java.time.Instant

/**
 * Zgłoszenie z pomiaru „Próg” w budynku C-13, wysłane w południe [day] października 2026 (dzień dwucyfrowy).
 *
 * @param id identyfikator zgłoszenia
 * @param day dzień października
 */
internal fun queued(id: String, day: Int): Submission =
    Submission(
        id = id,
        version = 1,
        status = SubmissionStatus.SENT,
        createdAt = Instant.parse("2026-10-${day.toString().padStart(2, '0')}T12:00:00Z"),
        content =
            SubmissionContent(
                kind = SubmissionKind.PRECISE,
                location = GeoPoint(longitude = 17.06, latitude = 51.1),
                photoIds = listOf("p-$id"),
                buildingSymbol = "C-13",
                barrierGroup = BarrierGroup.HORIZONTAL_CIRCULATION,
                barrierType = "TB-13",
                severity = 2,
            ),
    )

/**
 * Zgłoszenie szybkie [id] z grupą „Komunikacja pionowa”, bez typu i uciążliwości, wysłane jak [queued].
 *
 * @param id identyfikator zgłoszenia
 */
internal fun quick(id: String): Submission =
    queued(id = id, day = 27).run {
        val asked = content.copy(barrierGroup = BarrierGroup.VERTICAL_CIRCULATION, barrierType = null, severity = null)
        copy(content = asked.copy(kind = SubmissionKind.QUICK))
    }
