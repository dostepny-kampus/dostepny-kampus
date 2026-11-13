/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.systemtest

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.dostepnykampus.core.model.BarrierGroup
import io.github.dostepnykampus.core.model.GeoMultiPolygon
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.core.model.SubmissionKind
import io.github.dostepnykampus.core.network.AccessTokens
import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.core.network.NetworkModule
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Zbierający zalogowany w realmie deweloperskim wysyła z aplikacji zgłoszenie szybkie na prawdziwy serwer,
 * a serwer zwraca je na liście zgłoszeń tego konta.
 *
 * Wymagania: BR-9, US8.1
 */
@RunWith(AndroidJUnit4::class)
class SubmissionToServerTest {
    private val tokens = AccessTokens()
    private val http = NetworkModule.okHttp(tokens)
    private val json = NetworkModule.json()

    /** Zgłoszenie zapisane na serwerze wraca w zgłoszeniach Zbierającego. */
    @Test
    fun submissionReachesServer() {
        runBlocking {
            tokens.signIn(collectorToken())
            val place = firstBuildingPoint()
            val id = UUID.randomUUID().toString()
            val content =
                SubmissionContent(
                    kind = SubmissionKind.QUICK,
                    location = place,
                    photoIds = listOf(UUID.randomUUID().toString()),
                    barrierGroup = BarrierGroup.OTHER,
                )
            val submissions = NetworkModule.submissionsApi(http, json)
            val created = submissions.create(id, content)
            assertTrue("Wysłanie zgłoszenia: $created", created is ApiResult.Success)
            val mine = submissions.mine()
            assertTrue(
                "Zgłoszenia konta: $mine",
                mine is ApiResult.Success && mine.value.any { it.id == id },
            )
        }
    }

    private suspend fun firstBuildingPoint(): GeoPoint {
        val list = NetworkModule.buildingsApi(http, json).listBuildings()
        check(list is ApiResult.Success) { "Lista budynków: $list" }
        val outline =
            list.value.buildings
                .first()
                .building
                .outline
        val polygons = if (outline is GeoMultiPolygon) outline.polygons else listOf(outline as GeoPolygon)
        val polygon = polygons.first()
        return polygon.rings
            .first()
            .first()
    }

    private fun collectorToken(): String {
        val form =
            FormBody
                .Builder()
                .add("grant_type", "password")
                .add("client_id", "dostepny-kampus-android")
                .add("username", "zbierajacy.test")
                .add("password", "haslo-testowe-zbierajacy")
                .build()
        val request =
            Request
                .Builder()
                .url(TOKEN_URL)
                .post(form)
                .build()
        val body = OkHttpClient().newCall(request).execute().use { it.body.string() }
        return Json
            .parseToJsonElement(body)
            .jsonObject
            .getValue("access_token")
            .jsonPrimitive
            .content
    }

    private companion object {
        const val TOKEN_URL = "http://10.0.2.2:8180/realms/dostepny-kampus/protocol/openid-connect/token"
    }
}
