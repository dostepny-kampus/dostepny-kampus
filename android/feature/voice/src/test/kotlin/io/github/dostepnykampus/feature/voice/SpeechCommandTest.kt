/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.voice

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Rozpoznawanie poleceń wspólnych dla kroków dialogu. */
class SpeechCommandTest {
    @Test
    fun recognizesCommandsAndVariants() {
        val examples =
            mapOf(
                "tak" to SpeechCommand.YES,
                "Tak, poproszę." to SpeechCommand.YES,
                "nie" to SpeechCommand.NO,
                "Nie, dzięki!" to SpeechCommand.NO,
                "nie, dziękuję" to SpeechCommand.NO,
                "anuluj" to SpeechCommand.CANCEL,
                "Proszę, anuluj zgłoszenie." to SpeechCommand.CANCEL,
                "powtórz" to SpeechCommand.REPEAT,
                "POWTÓRZ, proszę!" to SpeechCommand.REPEAT,
                "wróć" to SpeechCommand.BACK,
                "COFNIJ." to SpeechCommand.BACK,
            )
        examples.forEach { (phrase, command) ->
            assertEquals(command, recognizeSpeechCommand(phrase), "Polecenie: $phrase")
        }
    }

    @Test
    fun ignoresSilenceAndWordsInOtherUtterances() {
        listOf("", "  … ", "schody i windy", "nie anuluj", "tak, przy wejściu", "tak nie").forEach { phrase ->
            assertNull(recognizeSpeechCommand(phrase), "To nie jest samodzielne polecenie: $phrase")
        }
    }
}
