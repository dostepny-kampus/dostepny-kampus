/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.inspection.progress

/**
 * Czynności ekranu K1.
 *
 * Wymagania: US11.3
 * TODO(#1843): trasa C5 Koordynatora, do której prowadzi „Otwórz”.
 *
 * @property onBack „Wstecz” i „Wróć do mapy”
 * @property onOpen „Otwórz” przy budynku: C5 Koordynatora; argument to symbol budynku
 */
data class ProgressActions(val onBack: () -> Unit, val onOpen: (buildingSymbol: String) -> Unit)
