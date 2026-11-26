/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.voice

/** Atrapa [SpeechToText]: kolejne rozpoznania oddają kolejne [results] i zapisują nagrania w [recordings]. */
internal class FakeSpeechToText(vararg results: SpeechResult) : SpeechToText {
    private val remaining = ArrayDeque(results.toList())

    val recordings = mutableListOf<ShortArray>()

    override suspend fun recognize(samples: ShortArray): SpeechResult {
        recordings += samples
        return checkNotNull(remaining.removeFirstOrNull()) { "atrapa nie ma więcej wyników" }
    }
}
