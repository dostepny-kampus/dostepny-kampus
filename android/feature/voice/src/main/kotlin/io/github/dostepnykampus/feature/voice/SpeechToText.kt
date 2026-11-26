/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.voice

/** Wynik rozpoznania jednej wypowiedzi. */
sealed interface SpeechResult {
    /** Rozpoznana wypowiedź: [text] to transkrypcja bez normalizacji. */
    data class Recognized(val text: String) : SpeechResult

    /** W nagraniu nie ma mowy. */
    data object NoSpeech : SpeechResult

    /** Usługa rozpoznawania nie odpowiedziała, np. bez sieci. */
    data object Unavailable : SpeechResult
}

/** Rozpoznawanie mowy z nagrania jednej wypowiedzi. */
fun interface SpeechToText {
    /** Rozpoznaje [samples]: PCM 16 bitów, 16 kHz, mono. Brak mowy i niedostępna usługa to wyniki, nie wyjątki. */
    suspend fun recognize(samples: ShortArray): SpeechResult
}
