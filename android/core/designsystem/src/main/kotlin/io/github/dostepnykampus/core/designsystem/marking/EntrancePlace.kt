/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem.marking

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import io.github.dostepnykampus.core.designsystem.R
import io.github.dostepnykampus.core.model.building.Entrance
import java.util.Locale
import kotlin.math.roundToInt

private val POLISH = Locale.forLanguageTag("pl-PL")

/**
 * Odległość od wejścia słowami, np. „ok. 3 m od wejścia głównego”, bez współrzędnych; najmniej 1 m.
 *
 * Wymagania: US7.4, US8.4, NFR-19
 *
 * @param entrance najbliższe wejście
 * @param meters odległość od wejścia w metrach
 */
@Composable
@ReadOnlyComposable
public fun nearEntrance(entrance: Entrance, meters: Double): String {
    val name = entrance.name
    val which =
        when {
            name != null && entrance.hasName -> stringResource(R.string.miejsce_wejscie_nazwa, name)
            entrance.isMain -> stringResource(R.string.miejsce_wejscie_glowne)
            else -> stringResource(R.string.miejsce_wejscie)
        }
    return stringResource(R.string.miejsce_od, meters.roundToInt().coerceAtLeast(1), which)
}

/**
 * Miejsce pomiaru słowami: odległość od wejścia i dokładność, np. „ok. 3 m od wejścia głównego, dokładność 0,8 m”.
 *
 * Wymagania: US7.4, NFR-19
 *
 * @param near odległość od wejścia z [nearEntrance]; `null` daje samą dokładność
 * @param accuracyMeters dokładność pomiaru w metrach
 */
@Composable
@ReadOnlyComposable
public fun measuredPlace(near: String?, accuracyMeters: Double): String {
    val accuracy = String.format(POLISH, "%.1f", accuracyMeters)
    return if (near == null) {
        stringResource(R.string.miejsce_dokladnosc, accuracy)
    } else {
        stringResource(R.string.miejsce_z_dokladnoscia, near, accuracy)
    }
}
