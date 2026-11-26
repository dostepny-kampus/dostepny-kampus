/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.review.photo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import io.github.dostepnykampus.core.data.review.ReviewQueue
import io.github.dostepnykampus.core.data.review.Reviews
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.model.review.ReviewClaim
import io.github.dostepnykampus.core.model.review.ReviewDecision
import io.github.dostepnykampus.core.model.review.ReviewOutcome
import io.github.dostepnykampus.core.model.review.SimilarSubmission
import io.github.dostepnykampus.core.model.submission.Submission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream

/**
 * Stan zasłaniania: zdjęcie z serwera, zasłony po dotknięciu i przyciskach, zapis z wypalonymi zasłonami,
 * brak sieci przy zapisie i stany wspólne.
 *
 * Wymagania: BR-15, NFR-25, NFR-53
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CoverViewModelTest {
    private val sent = mutableListOf<ByteArray>()
    private var saved = 0

    @Test
    fun showsPhoto() =
        coverTest { model ->
            assertEquals("przed odpowiedzią serwera", CoverUiState.Loading, model.state.value)
            runCurrent()
            assertEquals("zdjęcie bez zasłon", CoverUiState.Ready(photo = CoverPhoto(PHOTO)), model.state.value)
        }

    @Test
    fun showsCommonStates() {
        val cases =
            listOf(
                ServerReply.Offline to CoverUiState.Offline,
                ServerReply.Fault(FORBIDDEN) to CoverUiState.Forbidden,
                ServerReply.Fault(SERVER_ERROR) to CoverUiState.ServerError,
                ServerReply.Missing to CoverUiState.ServerError,
            )
        cases.forEach { (reply, expected) ->
            val fake = Fake(photo = reply)
            coverTest(fake) { model ->
                runCurrent()
                assertEquals("stan dla $reply", expected, model.state.value)
                model.refresh()
                runCurrent()
                assertEquals("„Spróbuj ponownie” czyta zdjęcie od nowa", 2, fake.reads)
            }
        }
    }

    @Test
    fun changesCoversByTapAndButtons() =
        coverTest { model ->
            runCurrent()
            model.tap(x = CENTER, y = CENTER)
            model.bigger()
            val bigger = Covers().tap(x = CENTER, y = CENTER).bigger()
            assertEquals("większa zasłona", bigger, ready(model).covers)
            model.smaller()
            model.remove()
            assertEquals("zasłona usunięta", Covers(), ready(model).covers)
        }

    @Test
    fun savesBurnedPhoto() =
        coverTest { model ->
            runCurrent()
            model.tap(x = CENTER, y = CENTER)
            model.save { saved++ }
            assertTrue("„Zapisujemy zdjęcie…” w trakcie", ready(model).isSaving)
            runCurrent()
            val image = checkNotNull(decode(sent.single())) { "wysłane zdjęcie jest obrazem" }
            val middle = image.getPixel(SIDE / 2, SIDE / 2)
            assertTrue("zasłona wypalona w pikselach", brightness(middle) < DARK)
            assertEquals("powrót do R2 po zapisie", 1, saved)
        }

    @Test
    fun savesNothingWithoutCovers() =
        coverTest { model ->
            runCurrent()
            model.save { saved++ }
            runCurrent()
            assertEquals("bez zasłon nic nie idzie na serwer", 0, sent.size)
            assertEquals("powrót do R2", 1, saved)
        }

    @Test
    fun offlineSaveKeepsCovers() =
        coverTest(Fake(replaced = ServerReply.Offline)) { model ->
            runCurrent()
            model.tap(x = CENTER, y = CENTER)
            model.save { saved++ }
            runCurrent()
            val covers = Covers().tap(x = CENTER, y = CENTER)
            val expected = CoverUiState.Ready(photo = CoverPhoto(PHOTO), covers = covers, isOffline = true)
            assertEquals("komunikat braku sieci, zasłony zostają", expected, model.state.value)
            assertEquals("bez powrotu do R2", 0, saved)
        }

    @Test
    fun brokenPhotoShowsServerError() =
        coverTest(Fake(photo = ServerReply.Ok(byteArrayOf(1, 2, 3)))) { model ->
            runCurrent()
            model.tap(x = CENTER, y = CENTER)
            model.save { saved++ }
            runCurrent()
            assertEquals("zdjęcie, które nie jest obrazem", CoverUiState.ServerError, model.state.value)
            assertEquals("nic nie idzie na serwer", 0, sent.size)
        }

    private fun ready(model: CoverViewModel): CoverUiState.Ready = model.state.value as CoverUiState.Ready

    private fun coverTest(fake: Fake = Fake(), block: suspend TestScope.(CoverViewModel) -> Unit) {
        val dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        try {
            runTest(dispatcher) {
                block(CoverViewModel(submissionId = "a", photoId = "p-a", reviews = fake, computeContext = dispatcher))
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun decode(jpeg: ByteArray): Bitmap? = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)

    private fun brightness(pixel: Int): Int = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3

    private inner class Fake(
        private val photo: ServerReply<ByteArray> = ServerReply.Ok(PHOTO),
        private val replaced: ServerReply<Unit> = ServerReply.Ok(Unit),
    ) : Reviews {
        var reads = 0

        override suspend fun queue(): ServerReply<ReviewQueue> = ServerReply.Missing

        override suspend fun submission(submissionId: String): ServerReply<Submission> = ServerReply.Missing

        override fun photoUrl(submissionId: String, photoId: String): String = "$submissionId/$photoId"

        override suspend fun claim(submissionId: String): ServerReply<ReviewClaim> = ServerReply.Missing

        override suspend fun decide(submissionId: String, decision: ReviewDecision): ServerReply<ReviewOutcome> =
            ServerReply.Missing

        override suspend fun similar(submissionId: String): ServerReply<List<SimilarSubmission>> = ServerReply.Missing

        override suspend fun photo(submissionId: String, photoId: String): ServerReply<ByteArray> {
            reads++
            return photo
        }

        override suspend fun replacePhoto(submissionId: String, photoId: String, jpeg: ByteArray): ServerReply<Unit> {
            sent += jpeg
            return replaced
        }
    }

    private companion object {
        const val FORBIDDEN = 403
        const val SERVER_ERROR = 500
        const val SIDE = 100
        const val QUALITY = 95
        const val DARK = 40
        const val CENTER = 0.5f
        val PHOTO: ByteArray = whiteJpeg()

        fun whiteJpeg(): ByteArray {
            val bitmap = Bitmap.createBitmap(SIDE, SIDE, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
            return ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
                out.toByteArray()
            }
        }
    }
}
