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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.future.future
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
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.lang.management.ManagementFactory
import java.lang.management.ThreadMXBean
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.NANOSECONDS
import java.util.concurrent.TimeUnit.SECONDS

/**
 * Anulowanie każdej funkcji `suspend` obu klientów (NFR-55): serwer wstrzymuje nagłówki albo ciało odpowiedzi
 * na 5 s, wywołujący anuluje korutynę dopiero po potwierdzonym starcie żądania.
 *
 * Próg 500 ms bez zegara ściennego: klient przerywa żądanie HTTP w wątku anulującym, zanim `cancel` wróci, więc
 * przerwanie jest sprawdzane w chwili powrotu z anulowania. Czas do przerwania to tylko praca tego wątku, mierzona
 * czasem procesora wątku, do którego nie wliczają się pauzy GC ani oczekiwanie na procesor na obciążonym runnerze.
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

    @ParameterizedTest
    @ValueSource(strings = ["putPhoto", "photo"])
    fun nfr55_cancel_photo_client(call: String) {
        server.dispatcher = holdHeaders()
        val running = begin(call, client(PLAIN_TIMEOUT_MILLIS))
        assertNotNull(server.takeRequest(START_WAIT_SECONDS, SECONDS), "żądanie nie dotarło do serwera")
        assertNull(running.outcome.within(PLAIN_TIMEOUT_MILLIS * 3), "zdjęcie poszło przez zwykłego klienta")
        assertCancelled(running)
    }

    @ParameterizedTest
    @MethodSource("calls")
    fun nfr55_broken_body_no_cancel(call: String) {
        server.enqueue(MockResponse().setBody(BODY).setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY))
        val outcome = begin(call, client()).outcome.within(GUARD_WAIT_MILLIS)
        assertInstanceOf(ApiResult.NetworkError::class.java, outcome?.value)
    }

    @ParameterizedTest
    @MethodSource("plainCalls")
    fun nfr55_timeout_no_cancel(call: String) {
        server.dispatcher = holdHeaders()
        val outcome = begin(call, client(PLAIN_TIMEOUT_MILLIS)).outcome.within(GUARD_WAIT_MILLIS)
        assertInstanceOf(ApiResult.NetworkError::class.java, outcome?.value)
    }

    private fun assertCancelled(running: Running) {
        val cpuBefore = THREADS.currentThreadCpuTime
        running.task.cancel(true)
        val cpuMillis = NANOSECONDS.toMillis(THREADS.currentThreadCpuTime - cpuBefore)
        val isInterrupted = canceled.count == 0L
        val result = running.outcome.within(WAIT_MILLIS)?.value
        val slow = "anulowanie zajęło $cpuMillis ms procesora, limit $LIMIT_MILLIS ms"
        assertAll(
            { assertTrue(isInterrupted, "klient nie przerwał żądania HTTP przed powrotem z anulowania") },
            { assertTrue(cpuMillis <= LIMIT_MILLIS, slow) },
            { assertInstanceOf(CancellationException::class.java, result, "wywołujący nie dostał anulowania") },
        )
    }

    private fun begin(call: String, http: OkHttpClient): Running {
        val apis = Apis(http, server.url("/").toString())
        val outcome = CompletableDeferred<Finished>()
        val task =
            scope.future {
                try {
                    outcome.complete(Finished(CALLS.getValue(call)(apis)))
                } catch (cancelled: CancellationException) {
                    outcome.complete(Finished(cancelled))
                    throw cancelled
                }
            }
        return Running(task, outcome)
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
        response.newBuilder().body(readingBody(response.body)).build()

    private fun readingBody(body: ResponseBody): ResponseBody = ReadingBody(body, reading)

    private fun holdHeaders(): Dispatcher =
        object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                held.await(HOLD_SECONDS, SECONDS)
                return MockResponse().setBody(BODY)
            }
        }

    private data class Running(val task: CompletableFuture<*>, val outcome: CompletableDeferred<Finished>)

    private data class Finished(val value: Any)

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
        const val WAIT_MILLIS = 2_000L
        const val HOLD_SECONDS = 5L
        const val START_WAIT_SECONDS = 5L
        const val BODY_BYTES = 50
        const val STEP_BYTES = 1_024L
        const val STEPS = 50
        const val STEP_MILLIS = 100L
        const val PLAIN_TIMEOUT_MILLIS = 300L
        const val GUARD_WAIT_MILLIS = 5_000L
        const val PHOTO_CALL = "putPhoto"
        const val PHOTO_READ_CALL = "photo"
        val THREADS: ThreadMXBean = ManagementFactory.getThreadMXBean()
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
                PHOTO_READ_CALL to call { it.submissions.photo(ID, photoId = PHOTO) },
                "accepted" to call { it.submissions.accepted("C-13") },
            )

        @JvmStatic
        fun calls(): List<String> = CALLS.keys.toList()

        @JvmStatic
        fun plainCalls(): List<String> = CALLS.keys.filter { it != PHOTO_CALL && it != PHOTO_READ_CALL }

        fun call(block: suspend (Apis) -> Any): suspend (Apis) -> Any = block

        fun <T> CompletableDeferred<T>.within(millis: Long): T? = runBlocking { withTimeoutOrNull(millis) { await() } }

        @JvmStatic
        @BeforeAll
        fun warmUp() {
            val idle = CompletableDeferred<Unit>()
            CoroutineScope(SupervisorJob()).future { idle.await() }.cancel(true)
            idle.within(1)
        }
    }
}
