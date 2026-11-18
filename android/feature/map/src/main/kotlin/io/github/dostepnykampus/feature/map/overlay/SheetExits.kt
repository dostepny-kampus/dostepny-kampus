/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.overlay

import androidx.compose.runtime.Immutable

/**
 * Wyjścia z arkusza na mapie V1.
 *
 * Wymagania: US2.8, NFR-15, NFR-53
 *
 * @property onClose „Zamknij”
 * @property onBack systemowe „Wstecz”
 * @property onBuilding wiersz budynku o podanym symbolu
 */
@Immutable
internal data class SheetExits(
    val onClose: () -> Unit,
    val onBack: () -> Unit = onClose,
    val onBuilding: (String) -> Unit = {},
)
