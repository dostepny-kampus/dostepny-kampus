/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.bar.PinnedButton
import io.github.dostepnykampus.core.designsystem.bar.PinnedButtonKind
import io.github.dostepnykampus.feature.map.R
import io.github.dostepnykampus.feature.map.overlay.spokenEntranceTitle

/**
 * Czynności karty budynku: „Pokaż wejście na mapie” dla wejścia zalecanego, które TalkBack czyta z nazwą wejścia,
 * i „Pokaż budynek na mapie” w [CardMode.isWhole].
 *
 * Wymagania: US2.8, US2.7, NFR-10
 *
 * @param mode tryb karty; „Pokaż budynek na mapie” ma tylko karta na cały ekran
 * @param recommended wejście zalecane; `null` ukrywa „Pokaż wejście na mapie”
 * @param onShowEntrance „Pokaż wejście na mapie” z identyfikatorem wejścia
 * @param onShowOnMap „Pokaż budynek na mapie”
 */
@Composable
internal fun CardActions(
    mode: CardMode,
    recommended: OrderedEntrance?,
    onShowEntrance: (String) -> Unit,
    onShowOnMap: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CeleDotyku.odstep)) {
        recommended?.let { listed -> ShowEntranceButton(listed = listed, onShowEntrance = onShowEntrance) }
        if (mode.isWhole) {
            CardButton(stringResource(R.string.karta_pokaz), onShowOnMap, outlined = true)
        }
    }
}

@Composable
private fun ShowEntranceButton(listed: OrderedEntrance, onShowEntrance: (String) -> Unit) {
    val title = spokenEntranceTitle(listed.entrance.name, isFromData = listed.entrance.hasName)
    val spoken = stringResource(R.string.wizyta_pokaz_czytaj, title)
    PinnedButton(
        text = stringResource(R.string.wizyta_pokaz),
        onClick = { onShowEntrance(listed.entrance.id) },
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = spoken },
    )
}

@Composable
private fun CardButton(text: String, onClick: () -> Unit, outlined: Boolean = false) {
    val kind = if (outlined) PinnedButtonKind.Outlined else PinnedButtonKind.Filled
    PinnedButton(text = text, onClick = onClick, modifier = Modifier.fillMaxWidth(), kind = kind)
}
