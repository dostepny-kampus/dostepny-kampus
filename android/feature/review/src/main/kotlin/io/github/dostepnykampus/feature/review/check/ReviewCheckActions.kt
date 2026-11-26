/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.review.check

/**
 * Czynności ekranu R2.
 *
 * Wymagania: US10, US10.2
 *
 * @property onBack „Wstecz” i „Wróć do mapy”
 * @property onDecided powrót do R1 po zapisanej decyzji
 */
data class ReviewCheckActions(val onBack: () -> Unit, val onDecided: () -> Unit)
