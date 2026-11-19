/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import io.github.dostepnykampus.feature.map.R

/**
 * Nazwa wejścia do napisu: [name] w cudzysłowie, gdy to nazwa z danych ([isFromData]), inaczej bez cudzysłowu;
 * bez [name] słowo „Wejście”.
 *
 * Wymagania: BR-27, US4.6
 */
@Composable
@ReadOnlyComposable
internal fun entranceTitle(name: String?, isFromData: Boolean): String =
    when {
        name == null -> stringResource(R.string.karta_bez_nazwy)
        isFromData -> stringResource(R.string.wejscie_nazwa_z_danych, name)
        else -> name
    }
