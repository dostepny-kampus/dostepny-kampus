/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.voice

/** Wynik rozpoznania jednej wypowiedzi. */
public sealed interface SpeechResult {
    /** Rozpoznana wypowiedź: [text] to transkrypcja bez normalizacji. */
    public data class Recognized(public val text: String) : SpeechResult

    /** W nagraniu nie ma mowy. */
    public data object NoSpeech : SpeechResult

    /** Usługa rozpoznawania nie odpowiedziała, np. bez sieci. */
    public data object Unavailable : SpeechResult
}

/** Rozpoznawanie mowy z nagrania jednej wypowiedzi. */
public fun interface SpeechToText {
    /** Rozpoznaje [samples]: PCM 16 bitów, 16 kHz, mono. Brak mowy i niedostępna usługa to wyniki, nie wyjątki. */
    public suspend fun recognize(samples: ShortArray): SpeechResult
}
