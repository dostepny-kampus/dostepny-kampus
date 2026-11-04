/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import io.github.dostepnykampus.core.model.BarrierGroup
import io.github.dostepnykampus.core.model.EntranceMarking
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.Submission
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.core.model.SubmissionFloor
import io.github.dostepnykampus.core.model.SubmissionKind
import io.github.dostepnykampus.core.model.SubmissionStatus
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import java.time.Instant

/**
 * Serwer zgłoszeń na MockWebServer z parserem z [NetworkModule] i treściami zbudowanymi z pól schematów
 * `SubmissionContent`, `Submission` i `VersionConflictProblem` z `api/openapi.yaml`.
 */
internal class SubmissionServer {
    private val server = MockWebServer()
    private val json = NetworkModule.json()

    fun start() {
        server.start()
    }

    fun stop() {
        server.shutdown()
    }

    fun enqueue(code: Int, body: String) {
        server.enqueue(MockResponse().setResponseCode(code).setBody(body))
    }

    fun <T> blocking(call: suspend SubmissionsApi.() -> T): T =
        runBlocking { SubmissionsApi(OkHttpClient(), json, server.url("/").toString()).call() }

    fun sentBody(): JsonObject = parse(server.takeRequest().body.readUtf8())

    fun skipRequest() {
        server.takeRequest()
    }

    fun parse(text: String): JsonObject = json.parseToJsonElement(text).jsonObject

    companion object {
        const val ID = "0b6f6c1e-8a43-4d5e-9f43-2f1d3c4b5a69"
        const val OLD = "7a1e2f3c-9b8d-4c6e-a5f4-3d2c1b0a9e8f"
        const val PHOTO = "5d1c2b3a-4e5f-4a6b-8c7d-9e0f1a2b3c4d"
        const val POINT = """"location":{"type":"Point","coordinates":[17.06209,51.10785]}"""
        const val PRECISE =
            """"kind":"PRECISE",$POINT,"accuracyMeters":2.5,"manualCorrection":true,"buildingSymbol":"C-13",""" +
                """"floor":{"floorId":"C-13-1","number":1,"mezzanine":true},"barrierGroup":"OTHER",""" +
                """"barrierType":"TB-29","severity":3,"description":"Brak poręczy","replacesSubmissionId":"$OLD",""" +
                """"barrierRemoved":true,"entranceId":"node/1","entranceMarking":"INACCESSIBLE","photoIds":["$PHOTO"]"""
        const val SERVER_DATA =
            """"id":"$ID","version":2,"createdAt":"2026-11-03T18:00:00Z","duplicateOfSubmissionId":"$OLD",""" +
                """"author":{"pseudonym":"Autor 17","roles":["COLLECTOR"]},"""
        const val REASON = "Zdjęcie nie pokazuje bariery"
        const val REJECTED = """"status":"REJECTED","rejectionReason":"$REASON","""
        const val ACCEPTED = """"status":"ACCEPTED","verifiedAt":"2026-11-04T09:30:00Z","""
        const val PROBLEM = """"type":"about:blank","title":"Konflikt wersji","status":409,"detail":"Inna wersja""""

        val precise =
            SubmissionContent(
                kind = SubmissionKind.PRECISE,
                location = GeoPoint(longitude = 17.06209, latitude = 51.10785),
                photoIds = listOf(PHOTO),
                accuracyMeters = 2.5,
                isManualCorrection = true,
                buildingSymbol = "C-13",
                floor = SubmissionFloor(floorId = "C-13-1", number = 1, isMezzanine = true),
                barrierGroup = BarrierGroup.OTHER,
                barrierType = "TB-29",
                severity = 3,
                description = "Brak poręczy",
                replacesSubmissionId = OLD,
                isBarrierRemoved = true,
                entranceId = "node/1",
                entranceMarking = EntranceMarking.INACCESSIBLE,
            )
        val quick =
            SubmissionContent(
                kind = SubmissionKind.QUICK,
                location = GeoPoint(longitude = 17.06209, latitude = 51.10785),
                photoIds = listOf(PHOTO),
                barrierGroup = BarrierGroup.OTHER,
            )
        val rejected =
            Submission(
                id = ID,
                version = 2,
                status = SubmissionStatus.REJECTED,
                createdAt = Instant.parse("2026-11-03T18:00:00Z"),
                content = precise,
                rejectionReason = REASON,
            )

        fun submission(status: String): String = "{$SERVER_DATA$status$PRECISE}"
    }
}
