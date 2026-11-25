/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import io.github.dostepnykampus.core.model.review.ReviewDecision
import io.github.dostepnykampus.core.model.review.SimilarSubmission
import io.github.dostepnykampus.core.model.submission.SubmissionStatus
import io.github.dostepnykampus.core.network.SubmissionServer.Companion.OLD
import io.github.dostepnykampus.core.network.SubmissionServer.Companion.PRECISE
import io.github.dostepnykampus.core.network.SubmissionServer.Companion.SERVER_DATA
import io.github.dostepnykampus.core.network.SubmissionServer.Companion.rejected
import io.github.dostepnykampus.core.network.di.NetworkModule
import io.github.dostepnykampus.core.network.review.decodeSimilar
import io.github.dostepnykampus.core.network.review.encodeDecision
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.nio.file.Path

/**
 * Treść `ReviewDecision` zgodna ze schematem z `api/openapi.yaml` i odczyt listy `SimilarSubmission`.
 *
 * Wymagania: US10.1, US10.2, US10.3, BR-3, BR-13, BR-16
 */
class ReviewJsonTest {
    private val json = NetworkModule.json()

    @Test
    fun encodesDecisions() {
        val decisions =
            mapOf(
                ReviewDecision.AcceptQuick(version = 2, barrierType = "TB-3", severity = 2) to
                    """{"decision":"ACCEPT","version":2,"barrierType":"TB-3","severity":2}""",
                ReviewDecision.Accept(version = 1) to """{"decision":"ACCEPT","version":1}""",
                ReviewDecision.RejectDuplicate(version = 2, reason = "Duplikat", duplicateOf = OLD) to
                    """{"decision":"REJECT","version":2,"rejectionReason":"Duplikat",""" +
                    """"duplicateOfSubmissionId":"$OLD"}""",
                ReviewDecision.Reject(version = 3, reason = "Nieaktualne") to
                    """{"decision":"REJECT","version":3,"rejectionReason":"Nieaktualne"}""",
            )
        decisions.forEach { (decision, expected) ->
            val body = json.encodeDecision(decision)
            assertEquals(json.parseToJsonElement(expected), json.parseToJsonElement(body), "treść decyzji $decision")
            assertEquals(emptyList<String>(), SPEC.violations(DECISION, body), "decyzja zgodna z ReviewDecision")
        }
    }

    @Test
    fun decodesSimilar() {
        val body = "[${SUBMISSION.removeSuffix("}")},\"distanceMeters\":4.5}]"
        val sent = rejected.copy(status = SubmissionStatus.SENT, rejectionReason = null)
        val expected = listOf(SimilarSubmission(submission = sent, distanceMeters = 4.5))
        assertEquals(expected, json.decodeSimilar(body), "podobne zgłoszenie z odległością")
    }

    @Test
    fun decodesSimilarSample() {
        val sample = SPEC.sample(SPEC.node("/components/schemas/SimilarSubmission"))
        assertDoesNotThrow("przykład SimilarSubmission z kontraktu") { json.decodeSimilar("[$sample]") }
    }

    @Test
    fun rejectsSimilarWithoutDistance() {
        assertThrows<IllegalArgumentException>("podobne zgłoszenie bez odległości") {
            json.decodeSimilar("[$SUBMISSION]")
        }
    }

    private companion object {
        const val DECISION = "/components/schemas/ReviewDecision"
        const val SUBMISSION = """{$SERVER_DATA"status":"SENT",$PRECISE}"""
        val SPEC = OpenApiSpec(Path.of(requireNotNull(System.getProperty("openapi.file"))))
    }
}
