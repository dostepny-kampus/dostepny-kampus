/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network.review

import io.github.dostepnykampus.core.model.review.ReviewDecision
import io.github.dostepnykampus.core.model.review.SimilarSubmission
import io.github.dostepnykampus.core.network.decodeContract
import io.github.dostepnykampus.core.network.encodeContract
import io.github.dostepnykampus.core.network.submission.toModel
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import io.github.dostepnykampus.core.network.json.Submission as SubmissionDto

private const val DISTANCE = "distanceMeters"
private const val DECISION = "decision"
private const val ACCEPT = "ACCEPT"
private const val REJECT = "REJECT"
private const val REASON = "rejectionReason"

/**
 * Treść `ReviewDecision` dla `POST /submissions/{submissionId}/review`.
 *
 * TODO(#3796): wywołanie trasy decyzji w kliencie HTTP sprawdzania.
 *
 * Wymagania: US10.2, US10.3, BR-3
 *
 * @param decision decyzja Weryfikatora
 */
internal fun Json.encodeDecision(decision: ReviewDecision): String =
    encodeContract(
        JsonObject.serializer(),
        buildJsonObject {
            put("version", decision.version)
            when (decision) {
                is ReviewDecision.Accept -> {
                    put(DECISION, ACCEPT)
                }

                is ReviewDecision.AcceptQuick -> {
                    put(DECISION, ACCEPT)
                    put("barrierType", decision.barrierType)
                    put("severity", decision.severity)
                }

                is ReviewDecision.Reject -> {
                    put(DECISION, REJECT)
                    put(REASON, decision.reason)
                }

                is ReviewDecision.RejectDuplicate -> {
                    put(DECISION, REJECT)
                    put(REASON, decision.reason)
                    put("duplicateOfSubmissionId", decision.duplicateOf)
                }
            }
        },
    )

/**
 * Lista `SimilarSubmission` z odpowiedzi serwera. Brak odległości albo zła treść zgłoszenia daje
 * [IllegalArgumentException].
 *
 * TODO(#3796): odczyt odpowiedzi trasy podobnych zgłoszeń w kliencie HTTP sprawdzania.
 *
 * Wymagania: US10.1, BR-13
 *
 * @param body tekst odpowiedzi z tablicą podobnych zgłoszeń
 */
internal fun Json.decodeSimilar(body: String): List<SimilarSubmission> {
    val lenient =
        Json(from = this) {
            explicitNulls = false
            ignoreUnknownKeys = true
        }
    return lenient.decodeContract(ListSerializer(JsonObject.serializer()), body).map { item ->
        val distance = requireNotNull(item[DISTANCE]) { "Podobne zgłoszenie bez odległości" }
        SimilarSubmission(
            submission = lenient.decodeFromJsonElement(SubmissionDto.serializer(), item).toModel(),
            distanceMeters = distance.jsonPrimitive.double,
        )
    }
}
