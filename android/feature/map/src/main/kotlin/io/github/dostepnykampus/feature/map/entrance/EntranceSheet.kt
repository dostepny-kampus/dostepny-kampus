/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.entrance

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.designsystem.Ikony
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.core.designsystem.bar.PinnedButton
import io.github.dostepnykampus.core.designsystem.marking.mapMarkingImage
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.feature.map.R
import io.github.dostepnykampus.feature.map.overlay.MapSheet
import io.github.dostepnykampus.feature.map.overlay.MapSheetHeader
import kotlin.math.ceil
import io.github.dostepnykampus.core.designsystem.R as DesignR

/**
 * Arkusz wejścia [entrance] na dole mapy V1: znak oznaczenia przy nagłówku z budynkiem, słowo oznaczenia, nazwa
 * wejścia, gdy jest w danych, i przejście do V3. Nagłówek dostaje fokus po otwarciu, a treść się przewija.
 *
 * TODO(#2918): podłącz arkusz do dotknięcia znaku wejścia na mapie.
 *
 * Wymagania: US2.1, US3.3, US17.1, NFR-18, NFR-20, NFR-21, NFR-53
 *
 * @param entrance wejście z katalogu
 * @param onClose „Zamknij” zostawia samą mapę
 * @param onEntrancesClick V3 budynku wejścia o podanym symbolu
 * @param modifier położenie arkusza na mapie
 */
@Composable
internal fun EntranceSheet(
    entrance: Entrance,
    onClose: () -> Unit,
    onEntrancesClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    MapSheet(modifier) {
        MapSheetHeader(
            title = stringResource(R.string.mapa_wejscie_naglowek, entrance.buildingSymbol),
            onClose = onClose,
        ) { MarkingSign(entrance.marking) }
        Text(
            text = markingLine(entrance),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = Typografia.wyroznienieWaga,
        )
        entrance.name?.let { Text(text = it, style = MaterialTheme.typography.bodyLarge) }
        if (entrance.marking == EntranceMarking.NO_DATA) {
            Text(
                text = stringResource(R.string.mapa_wejscie_niesprawdzone),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PinnedButton(
            text = stringResource(R.string.karta_sprawdz),
            onClick = { onEntrancesClick(entrance.buildingSymbol) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MarkingSign(marking: EntranceMarking) {
    val colors = DostepnyKampusTheme.mapColors
    val density = LocalDensity.current.density
    val size = Ikony.znakMapy
    val image =
        remember(key1 = marking, key2 = colors, key3 = density) {
            mapMarkingImage(marking = marking, sizePx = ceil(size.value * density).toInt(), colors = colors)
        }
    Image(bitmap = image.asImageBitmap(), contentDescription = null, modifier = Modifier.size(size))
}

@Composable
@ReadOnlyComposable
private fun markingLine(entrance: Entrance): String {
    val word =
        stringResource(
            when (entrance.marking) {
                EntranceMarking.ACCESSIBLE -> DesignR.string.oznaczenie_dostepne
                EntranceMarking.INACCESSIBLE -> DesignR.string.oznaczenie_niedostepne
                EntranceMarking.NO_DATA -> DesignR.string.oznaczenie_brak_danych
            },
        )
    return if (entrance.isMain) stringResource(R.string.mapa_wejscie_glowne, word) else word
}
