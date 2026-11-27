/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.voice

/** Polecenie rozpoznane niezależnie od kroku dialogu. */
enum class SpeechCommand {
    /** Potwierdzenie. */
    YES,

    /** Odmowa. */
    NO,

    /** Przerwanie dialogu. */
    CANCEL,

    /** Powtórzenie pytania. */
    REPEAT,

    /** Powrót do poprzedniego kroku. */
    BACK,
}

/**
 * Rozpoznaje całe polecenie w transkrypcji [text]. Zwraca `null` dla ciszy i dla wypowiedzi,
 * w której słowo polecenia jest tylko częścią innej odpowiedzi.
 *
 * TODO(#1524): podłączyć polecenia do kroków dialogu.
 */
fun recognizeSpeechCommand(text: String): SpeechCommand? {
    val phrase = normalizeSpeechText(text).removePrefix("prosze ").removeSuffix(" prosze")
    return when (phrase) {
        "tak", "tak poprosze" -> SpeechCommand.YES
        "nie", "nie dzieki", "nie dziekuje" -> SpeechCommand.NO
        "anuluj", "anuluj zgloszenie" -> SpeechCommand.CANCEL
        "powtorz", "powtorz pytanie" -> SpeechCommand.REPEAT
        "wroc", "cofnij" -> SpeechCommand.BACK
        else -> null
    }
}
