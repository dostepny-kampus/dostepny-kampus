/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.systemtest

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.dostepnykampus.core.model.BarrierGroup
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.core.model.SubmissionKind
import io.github.dostepnykampus.core.model.SubmissionStatus
import io.github.dostepnykampus.core.network.AccessTokens
import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.core.network.NetworkModule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED
import java.util.UUID

/** Zapisuje zgłoszenie klientem aplikacji i odczytuje je z prawdziwego serwera jako zalogowany autor. */
@RunWith(AndroidJUnit4::class)
class SubmissionsFromServerTest {
    @Test
    fun createAndReadOwnSubmission() {
        runBlocking {
            val tokens = AccessTokens()
            val api = NetworkModule.submissionsApi(NetworkModule.okHttp(tokens), NetworkModule.json())
            val id = UUID.randomUUID().toString()
            val content =
                SubmissionContent(
                    kind = SubmissionKind.QUICK,
                    location = GeoPoint(longitude = 17.0, latitude = 51.0),
                    photoIds = listOf(UUID.randomUUID().toString()),
                    barrierGroup = BarrierGroup.OTHER,
                )
            tokens.signIn(collectorToken(host = "10.0.2.2"))
            assertEquals(ApiResult.ServerError(HTTP_UNAUTHORIZED), api.create(id, content))
            tokens.signIn(collectorToken(host = "localhost"))
            val created = api.create(id, content)
            assertTrue("Zapis zgłoszenia: $created", created is ApiResult.Success)
            val saved = (created as ApiResult.Success).value
            assertEquals(id, saved.id)
            assertEquals(1, saved.version)
            assertEquals(SubmissionStatus.SENT, saved.status)
            assertEquals(content.kind, saved.content.kind)
            assertEquals(content.location, saved.content.location)
            assertEquals(content.photoIds, saved.content.photoIds)
            assertEquals(content.barrierGroup, saved.content.barrierGroup)
            val mine = api.mine()
            assertTrue("Odczyt własnych zgłoszeń: $mine", mine is ApiResult.Success)
            val rows = (mine as ApiResult.Success).value
            assertEquals(saved, rows.single { it.id == id })
        }
    }
}
