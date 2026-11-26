/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.systemtest

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.dostepnykampus.core.model.GeoMultiPolygon
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.model.submission.BarrierGroup
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.core.network.auth.AccessTokens
import io.github.dostepnykampus.core.network.di.NetworkModule
import kotlinx.coroutines.runBlocking
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
            tokens.signIn(realmToken(login = "zbierajacy.test", password = "haslo-testowe-zbierajacy"))
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
}
