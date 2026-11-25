/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.photo

/** Wolne miejsce w telefonie na zdjęcia zgłoszeń. Wymagania: NFR-7 */
public fun interface PhotoSpace {
    /** `true`, gdy w pamięci danych aplikacji zostało mniej niż 100 MB; błąd odczytu daje `false`. */
    public fun isLow(): Boolean
}

/** Najmniej wolnych bajtów, przy których telefon nie ostrzega o miejscu (100 MB). */
internal const val LOW_SPACE_BYTES: Long = 100_000_000L

/** Czy [availableBytes] wolnych bajtów to za mało na dalsze zdjęcia. Wymagania: NFR-7 */
internal fun isLowSpace(availableBytes: Long): Boolean = availableBytes < LOW_SPACE_BYTES

/**
 * [PhotoSpace] z liczbą wolnych bajtów z [availableBytes]; wyjątek przy odczycie daje brak ostrzeżenia.
 *
 * Wymagania: NFR-7
 */
internal class StoragePhotoSpace(private val availableBytes: () -> Long) : PhotoSpace {
    override fun isLow(): Boolean = runCatching(availableBytes).map(::isLowSpace).getOrDefault(false)
}
