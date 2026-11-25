/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics

/**
 * Stan wspólny ekranu roli pracowniczej, który potrzebuje serwera.
 *
 * Wymagania: NFR-50, US14.1
 */
public enum class StaffProblem(
    @param:StringRes internal val message: Int,
    @param:StringRes internal val action: Int,
    @param:StringRes internal val headline: Int? = null,
) {
    /** Brak sieci; czynność ponawia żądanie. */
    OFFLINE(R.string.praca_wymaga_internetu, R.string.sprobuj_ponownie, R.string.bez_internetu),

    /** Błąd serwera; czynność ponawia żądanie. */
    SERVER(R.string.praca_blad_serwera, R.string.sprobuj_ponownie),

    /** Rolę odebrano w trakcie pracy; czynność wraca do mapy. */
    FORBIDDEN(R.string.praca_brak_uprawnien, R.string.praca_wroc_do_mapy),
}

/**
 * Stan wspólny [problem]: nagłówek przy braku sieci, komunikat i przycisk czynności.
 *
 * Tokeny: wcięcie [Odstepy.m], odstęp [Odstepy.s], nagłówek `titleMedium`, komunikat `bodyLarge`.
 *
 * @param problem rodzaj stanu
 * @param onAction „Spróbuj ponownie” albo „Wróć do mapy”
 * @param modifier wymiary nadane przez ekran
 */
@Composable
public fun StaffProblemMessage(problem: StaffProblem, onAction: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(Odstepy.m), verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        problem.headline?.let { Text(stringResource(it), style = MaterialTheme.typography.titleMedium) }
        Text(stringResource(problem.message), style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onAction) { Text(stringResource(problem.action)) }
    }
}

/** Wynik czynności pracownika z nagłówkiem i opcjonalnym objaśnieniem, ogłaszany przez TalkBack. */
@Composable
public fun StaffResultMessage(title: String, modifier: Modifier = Modifier, message: String? = null) {
    Surface(
        modifier = modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(modifier = Modifier.padding(Odstepy.m), verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
            Text(text = title, fontWeight = Typografia.wyroznienieWaga, style = MaterialTheme.typography.titleLarge)
            if (message != null) Text(text = message, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
