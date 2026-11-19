/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.overlay

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import io.github.dostepnykampus.feature.map.R

/**
 * Nazwa wejścia do widocznego napisu: [name] w cudzysłowie „…”, gdy to nazwa z danych ([isFromData]), inaczej bez
 * cudzysłowu; bez [name] słowo „Wejście”.
 *
 * Wymagania: BR-27, US4.6
 */
@Composable
@ReadOnlyComposable
internal fun entranceTitle(name: String?, isFromData: Boolean): String =
    titleOf(name = name, isFromData = isFromData, quoted = R.string.wejscie_nazwa_z_danych)

/**
 * Nazwa wejścia do opisu dla TalkBack: jak [entranceTitle], ale nazwa z danych stoi w cudzysłowie »…«.
 *
 * Wymagania: BR-27, NFR-18
 */
@Composable
@ReadOnlyComposable
internal fun spokenEntranceTitle(name: String?, isFromData: Boolean): String =
    titleOf(name = name, isFromData = isFromData, quoted = R.string.wejscie_nazwa_czytaj)

@Composable
@ReadOnlyComposable
private fun titleOf(name: String?, isFromData: Boolean, @StringRes quoted: Int): String =
    when {
        name == null -> stringResource(R.string.karta_bez_nazwy)
        isFromData -> stringResource(quoted, name)
        else -> name
    }
