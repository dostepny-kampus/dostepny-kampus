/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.voice

import io.github.dostepnykampus.feature.voice.SpeechResult.NoSpeech
import io.github.dostepnykampus.feature.voice.SpeechResult.Recognized
import io.github.dostepnykampus.feature.voice.SpeechResult.Unavailable
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class FakeSpeechToTextTest {
    @Test
    fun returnsResultsInOrder() =
        runTest {
            val speech = FakeSpeechToText(Recognized("za wąskie drzwi"), NoSpeech, Unavailable)

            val results = List(3) { speech.recognize(shortArrayOf()) }

            assertEquals(listOf(Recognized("za wąskie drzwi"), NoSpeech, Unavailable), results, "kolejność wyników")
        }

    @Test
    fun keepsRecordings() =
        runTest {
            val speech = FakeSpeechToText(NoSpeech)
            val samples = shortArrayOf(1, 2, 3)

            speech.recognize(samples)

            assertSame(samples, speech.recordings.single(), "atrapa pamięta podane nagranie")
        }

    @Test
    fun failsWithoutMoreResults() {
        val speech = FakeSpeechToText()

        assertThrows(IllegalStateException::class.java, { runTest { speech.recognize(shortArrayOf()) } }, "bez wyniku")
    }
}
