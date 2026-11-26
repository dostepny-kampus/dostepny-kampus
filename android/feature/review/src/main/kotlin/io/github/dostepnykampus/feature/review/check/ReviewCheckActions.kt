/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.review.check

/**
 * Czynności ekranu R2.
 *
 * Wymagania: US10, US10.2, BR-15
 *
 * @property onBack „Wstecz” i „Wróć do mapy”
 * @property onDecided powrót do R1 po zapisanej decyzji
 * @property onCover „Zasłoń twarz lub tablicę” przy zdjęciu o identyfikatorze i adresie
 */
data class ReviewCheckActions(
    val onBack: () -> Unit,
    val onDecided: () -> Unit,
    val onCover: (photoId: String, photoUrl: String) -> Unit,
)
