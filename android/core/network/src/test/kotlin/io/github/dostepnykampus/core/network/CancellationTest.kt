/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import io.github.dostepnykampus.core.network.SubmissionServer.Companion.ID
import io.github.dostepnykampus.core.network.SubmissionServer.Companion.PHOTO
import io.github.dostepnykampus.core.network.SubmissionServer.Companion.quick
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.future.future
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import okio.BufferedSource
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS

/**
 * Anulowanie każdej funkcji `suspend` obu klientów w czasie rzeczywistym (NFR-55): serwer wstrzymuje nagłówki
 * albo ciało odpowiedzi na 5 s, wywołujący anuluje korutynę dopiero po potwierdzonym starcie żądania.
 */
class CancellationTest {
    private val server = MockWebServer()
    private val held = CountDownLatch(1)
    private val canceled = CountDownLatch(1)
    private val reading = CountDownLatch(1)
    private val scope = CoroutineScope(SupervisorJob())
    private val listener =
        object : EventListener() {
            override fun canceled(call: Call) {
                canceled.countDown()
            }
        }

    @BeforeEach
    fun start() {
        server.start()
    }

    @AfterEach
    fun stop() {
        held.countDown()
        scope.cancel()
        server.shutdown()
    }

    @ParameterizedTest
    @MethodSource("calls")
    fun nfr55_cancel_headers(call: String) {
        server.dispatcher = holdHeaders()
        val running = begin(call, client())
        assertNotNull(server.takeRequest(START_WAIT_SECONDS, SECONDS), "żądanie nie dotarło do serwera")
        assertCancelled(running)
    }

    @ParameterizedTest
    @MethodSource("calls")
    fun nfr55_cancel_body(call: String) {
        server.enqueue(
            MockResponse()
                .setBody(SLOW_BODY)
                .throttleBody(bytesPerPeriod = STEP_BYTES, period = STEP_MILLIS, unit = MILLISECONDS),
        )
        val running = begin(call, client())
        assertTrue(reading.await(START_WAIT_SECONDS, SECONDS), "klient nie zaczął czytać odpowiedzi")
        assertCancelled(running)
    }

    @Test
    fun nfr55_cancel_photo_client() {
        server.dispatcher = holdHeaders()
        val running = begin(PHOTO_CALL, client(PLAIN_TIMEOUT_MILLIS))
        assertNotNull(server.takeRequest(START_WAIT_SECONDS, SECONDS), "żądanie nie dotarło do serwera")
        assertNull(running.outcome.within(PLAIN_TIMEOUT_MILLIS * 3), "zdjęcie poszło przez zwykłego klienta")
        assertCancelled(running)
    }

    @ParameterizedTest
    @MethodSource("calls")
    fun nfr55_broken_body_no_cancel(call: String) {
        server.enqueue(MockResponse().setBody(BODY).setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY))
        val outcome = begin(call, client()).outcome.within(GUARD_WAIT_MILLIS)
        assertInstanceOf(ApiResult.NetworkError::class.java, outcome)
    }

    @ParameterizedTest
    @MethodSource("plainCalls")
    fun nfr55_timeout_no_cancel(call: String) {
        server.dispatcher = holdHeaders()
        val outcome = begin(call, client(PLAIN_TIMEOUT_MILLIS)).outcome.within(GUARD_WAIT_MILLIS)
        assertInstanceOf(ApiResult.NetworkError::class.java, outcome)
    }

    private fun assertCancelled(running: Running) {
        running.job.cancel()
        val outcome = running.outcome.within(LIMIT_MILLIS)
        assertAll(
            { assertNotNull(outcome, "wywołanie trwa dłużej niż $LIMIT_MILLIS ms po anulowaniu") },
            { assertTrue(canceled.await(LIMIT_MILLIS, MILLISECONDS), "klient nie przerwał żądania HTTP") },
            { assertInstanceOf(CancellationException::class.java, outcome, "wywołujący nie dostał anulowania") },
        )
    }

    private fun begin(call: String, http: OkHttpClient): Running {
        val apis = Apis(http, server.url("/").toString())
        val running = Running(Job(scope.coroutineContext.job), CompletableDeferred())
        scope.future(running.job) {
            try {
                running.outcome.complete(CALLS.getValue(call)(apis))
            } catch (cancelled: CancellationException) {
                running.outcome.complete(cancelled)
                throw cancelled
            }
        }
        return running
    }

    private fun client(callTimeoutMillis: Long = 0): OkHttpClient =
        OkHttpClient
            .Builder()
            .callTimeout(callTimeoutMillis, MILLISECONDS)
            .eventListener(listener)
            .addInterceptor(Interceptor(::markReading))
            .build()

    private fun markReading(chain: Interceptor.Chain): Response = chain.proceed(chain.request()).run(::withReadingBody)

    private fun withReadingBody(response: Response): Response =
        response.newBuilder().body(readingBody(checkNotNull(response.body))).build()

    private fun readingBody(body: ResponseBody): ResponseBody = ReadingBody(body, reading)

    private fun holdHeaders(): Dispatcher =
        object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                held.await(HOLD_SECONDS, SECONDS)
                return MockResponse().setBody(BODY)
            }
        }

    private fun CompletableDeferred<Any>.within(millis: Long): Any? =
        runBlocking { withTimeoutOrNull(millis) { await() } }

    private data class Running(val job: Job, val outcome: CompletableDeferred<Any>)

    private class ReadingBody(private val delegate: ResponseBody, private val reading: CountDownLatch) :
        ResponseBody() {
        private val source = delegate.source()

        override fun contentType(): MediaType? = delegate.contentType()

        override fun contentLength(): Long = delegate.contentLength()

        override fun source(): BufferedSource {
            reading.countDown()
            return source
        }
    }

    private class Apis(http: OkHttpClient, baseUrl: String) {
        val buildings = BuildingsApi(http, NetworkModule.json(), baseUrl)
        val submissions = SubmissionsApi(http, NetworkModule.json(), baseUrl)
    }

    private companion object {
        const val LIMIT_MILLIS = 500L
        const val HOLD_SECONDS = 5L
        const val START_WAIT_SECONDS = 5L
        const val BODY_BYTES = 50
        const val STEP_BYTES = 1_024L
        const val STEPS = 50
        const val STEP_MILLIS = 100L
        const val PLAIN_TIMEOUT_MILLIS = 300L
        const val GUARD_WAIT_MILLIS = 5_000L
        const val PHOTO_CALL = "putPhoto"
        val BODY = "0".repeat(BODY_BYTES)
        val SLOW_BODY = "0".repeat(STEP_BYTES.toInt() * STEPS)
        val JPEG = byteArrayOf(-1, -40)
        val CALLS: Map<String, suspend (Apis) -> Any> =
            mapOf(
                "listBuildings" to call { it.buildings.listBuildings() },
                "building" to call { it.buildings.building("C-13") },
                "locate" to call { it.buildings.locate(latitude = 51.10785, longitude = 17.06209) },
                "create" to call { it.submissions.create(ID, quick) },
                "update" to call { it.submissions.update(ID, 1, quick) },
                PHOTO_CALL to call { it.submissions.putPhoto(ID, photoId = PHOTO, jpeg = JPEG) },
                "accepted" to call { it.submissions.accepted("C-13") },
            )

        @JvmStatic
        fun calls(): List<String> = CALLS.keys.toList()

        @JvmStatic
        fun plainCalls(): List<String> = CALLS.keys.filter { it != PHOTO_CALL }

        fun call(block: suspend (Apis) -> Any): suspend (Apis) -> Any = block
    }
}
