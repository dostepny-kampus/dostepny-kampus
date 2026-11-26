/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.review.photo

/**
 * Czynności ekranu zasłaniania.
 *
 * Wymagania: BR-15, NFR-53
 *
 * @property onBack „Wstecz”: powrót do R2 bez zmian
 * @property onTap dotknięcie zdjęcia we współrzędnych względnych
 * @property onBigger „Większy”
 * @property onSmaller „Mniejszy”
 * @property onRemove „Usuń zasłonę”
 * @property onSave „Zapisz zdjęcie”
 * @property onRetry „Spróbuj ponownie”
 */
data class CoverActions(
    val onBack: () -> Unit,
    val onTap: (x: Float, y: Float) -> Unit,
    val onBigger: () -> Unit,
    val onSmaller: () -> Unit,
    val onRemove: () -> Unit,
    val onSave: () -> Unit,
    val onRetry: () -> Unit,
)
