/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.systemtest

import android.graphics.Bitmap
import android.graphics.Color
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.inspection.BuildingProgress
import io.github.dostepnykampus.core.model.review.RejectionReasons
import io.github.dostepnykampus.core.model.review.ReviewClaim
import io.github.dostepnykampus.core.model.review.ReviewDecision
import io.github.dostepnykampus.core.model.submission.BarrierGroup
import io.github.dostepnykampus.core.model.submission.Submission
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionFloor
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.core.network.auth.AccessTokens
import io.github.dostepnykampus.core.network.di.NetworkModule
import io.github.dostepnykampus.core.network.inspection.MarkerChange
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import java.io.ByteArrayOutputStream
import java.util.UUID

private const val PHOTO_SIDE = 64
private const val JPEG_QUALITY = 90

/** Budynek drogi demo. */
internal const val DEMO_BUILDING = "A-1"

/** Opis zgłoszenia TB-13 „próg” z założeń scenariusza; po nim powtórne uruchomienie znajduje to zgłoszenie. */
internal const val THRESHOLD_NOTE = "Próg przy drzwiach, inna bariera niż schody (założenie drogi demo)"

/** Miejsce TB-13: 5 m na wschód od średniej trasy pokazu, czyli od miejsca TB-1 po pomiarze. */
internal val THRESHOLD_PLACE = GeoPoint(latitude = 51.1080604, longitude = 17.0614921)

/**
 * Serwer drogi demo przez API, z kontami Zbierającego, Weryfikatora i Koordynatora realmu deweloperskiego:
 * założenia scenariusza, które nie są krokami w aplikacji, oraz odczyty bez ekranu. Założenia działają także przy
 * powtórnym uruchomieniu na tym samym serwerze: TB-13 jest szukane po opisie, znaczniki pięter się nie sumują,
 * a stare TB-1 z poprzednich uruchomień wychodzą z kolejki Weryfikatora.
 *
 * Wymagania: US10.1, US11.1
 */
internal class DemoServer {
    private val json = NetworkModule.json()
    private val collector by lazy { session(login = "zbierajacy.test", password = "haslo-testowe-zbierajacy") }
    private val verifier by lazy { session(login = "weryfikator.test", password = "haslo-testowe-weryfikator") }
    private val coordinator by lazy { session(login = "koordynator.test", password = "haslo-testowe-koordynator") }

    /** Założenia scenariusza: kolejka bez starych TB-1 z A-1, przyjęte TB-13 i 4 z 5 pięter A-1 oznaczone. */
    fun prepare() {
        runBlocking {
            closeOldReports()
            acceptThreshold()
            markFloors()
        }
    }

    /** Przyjęte zgłoszenia z A-1 o opisie [note]. */
    fun accepted(note: String): List<Submission> = runBlocking { find(note) }

    /** Postęp inwentaryzacji A-1 w odczycie Koordynatora. */
    fun progress(): BuildingProgress =
        runBlocking {
            val all = NetworkModule.inspectionProgressApi(coordinator, json).progress()
            check(all is ApiResult.Success) { "Postęp inwentaryzacji: $all" }
            all.value.first { it.buildingSymbol == DEMO_BUILDING }
        }

    private suspend fun find(note: String): List<Submission> {
        val list = NetworkModule.submissionsApi(collector, json).accepted(DEMO_BUILDING)
        check(list is ApiResult.Success) { "Przyjęte zgłoszenia $DEMO_BUILDING: $list" }
        return list.value.filter { it.content.description == note }
    }

    private suspend fun closeOldReports() {
        val reviews = NetworkModule.reviewApi(verifier, json)
        val queue = reviews.queue()
        check(queue is ApiResult.Success) { "Kolejka Weryfikatora: $queue" }
        queue.value
            .filter { it.content.barrierType == STAIRS && it.content.buildingSymbol == DEMO_BUILDING }
            .forEach { old ->
                val claim = reviews.claim(old.id)
                val taken = (claim as? ApiResult.Success)?.value as? ReviewClaim.Claimed
                taken?.let { mine ->
                    val version = mine.submission.version
                    val decision = ReviewDecision.Reject(version = version, reason = RejectionReasons.OUTDATED)
                    reviews.decide(old.id, decision)
                }
            }
    }

    private suspend fun acceptThreshold() {
        if (find(THRESHOLD_NOTE).isNotEmpty()) return
        val id = UUID.randomUUID().toString()
        val photo = UUID.randomUUID().toString()
        val submissions = NetworkModule.submissionsApi(collector, json)
        val created = submissions.create(id, thresholdContent(photo))
        check(created is ApiResult.Success) { "Zgłoszenie TB-13: $created" }
        val sent = submissions.putPhoto(submissionId = id, photoId = photo, jpeg = grayJpeg())
        check(sent is ApiResult.Success) { "Zdjęcie TB-13: $sent" }
        val reviews = NetworkModule.reviewApi(verifier, json)
        val claim = reviews.claim(id)
        val taken = (claim as? ApiResult.Success)?.value as? ReviewClaim.Claimed
        checkNotNull(taken) { "Wzięcie TB-13: $claim" }
        val decision = reviews.decide(id, ReviewDecision.Accept(version = taken.submission.version))
        check(decision is ApiResult.Success) { "Przyjęcie TB-13: $decision" }
    }

    private suspend fun markFloors() {
        val marks =
            (0 until MARKED_FLOORS).map { floor ->
                MarkerChange.Mark(
                    operationId = UUID.randomUUID().toString(),
                    markerId = UUID.randomUUID().toString(),
                    buildingSymbol = DEMO_BUILDING,
                    floorId = "$FLOOR_PREFIX$floor",
                )
            }
        val pushed = NetworkModule.markerChangesApi(collector, json).push(marks)
        check(pushed is ApiResult.Success && pushed.value.all { it.isApplied }) { "Znaczniki pięter: $pushed" }
    }

    private fun thresholdContent(photo: String): SubmissionContent =
        SubmissionContent(
            kind = SubmissionKind.PRECISE,
            location = THRESHOLD_PLACE,
            photoIds = listOf(photo),
            accuracyMeters = THRESHOLD_ACCURACY,
            buildingSymbol = DEMO_BUILDING,
            floor = SubmissionFloor(floorId = "${FLOOR_PREFIX}0"),
            barrierGroup = BarrierGroup.HORIZONTAL_CIRCULATION,
            barrierType = THRESHOLD,
            severity = 1,
            description = THRESHOLD_NOTE,
        )

    private fun session(login: String, password: String): OkHttpClient {
        val tokens = AccessTokens()
        tokens.signIn(realmToken(login = login, password = password))
        return NetworkModule.okHttp(tokens)
    }

    private companion object {
        const val STAIRS = "TB-1"
        const val THRESHOLD = "TB-13"
        const val FLOOR_PREFIX = "pokaz-a1-"
        const val MARKED_FLOORS = 4
        const val THRESHOLD_ACCURACY = 3.0
    }
}

/** Szare zdjęcie JPEG, które serwer przyjmuje jak zdjęcie z aparatu. */
internal fun grayJpeg(): ByteArray {
    val bitmap = Bitmap.createBitmap(PHOTO_SIDE, PHOTO_SIDE, Bitmap.Config.ARGB_8888)
    bitmap.eraseColor(Color.GRAY)
    val out = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
    return out.toByteArray()
}
