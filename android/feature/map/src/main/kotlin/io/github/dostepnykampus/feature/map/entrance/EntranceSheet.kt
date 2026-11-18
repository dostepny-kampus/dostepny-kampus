/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.entrance

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.designsystem.Ikony
import io.github.dostepnykampus.core.designsystem.LARGE_FONT_SCALE
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.core.designsystem.bar.PinnedButton
import io.github.dostepnykampus.core.designsystem.bar.PinnedButtonKind
import io.github.dostepnykampus.core.designsystem.marking.mapMarkingImage
import io.github.dostepnykampus.core.model.building.BuildingCatalog
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.feature.map.R
import io.github.dostepnykampus.feature.map.barrier.ShownBarriers
import io.github.dostepnykampus.feature.map.building.assessmentText
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import io.github.dostepnykampus.feature.map.overlay.BuildingLink
import io.github.dostepnykampus.feature.map.overlay.MapSheet
import io.github.dostepnykampus.feature.map.overlay.MapSheetHeader
import io.github.dostepnykampus.feature.map.overlay.SheetExits
import kotlin.math.ceil
import io.github.dostepnykampus.core.designsystem.R as DesignR

private val WIDE_WINDOW = 600.dp

/**
 * Arkusz wejścia o identyfikatorze [id] z [catalog] po dotknięciu jego znaku na mapie V1; bez [id] i bez takiego
 * wejścia w katalogu arkusza nie ma. Pokazane wejście trafia do [EntranceActions.onShow], a winda
 * z [EntranceActions.lift] do części „Za drzwiami”. Systemowe „Wstecz” wywołuje [SheetExits.onBack].
 *
 * Wymagania: US2.1, US3.3, NFR-53
 *
 * @param id identyfikator wejścia dotkniętego znaku; `null` bez wybranego wejścia
 * @param catalog katalog budynków i wejść; `null` przed jego wczytaniem
 * @param barriers bariery ze zgłoszeń przyjętych
 * @param exits „Zamknij”, „Wstecz” i wiersz budynku
 * @param actions czynności przy wejściu
 * @param modifier położenie arkusza na mapie
 * @param isFull arkusz na cały ekran, bez mapy
 */
@Composable
internal fun ChosenEntranceSheet(
    id: String?,
    catalog: BuildingCatalog?,
    barriers: ShownBarriers,
    exits: SheetExits,
    actions: EntranceActions,
    modifier: Modifier = Modifier,
    isFull: Boolean = false,
) {
    val found = remember(id, catalog, barriers) { id?.let { catalog?.shownEntrance(it, barriers.barriers) } }
    LaunchedEffect(found?.entrance) { found?.entrance?.let(actions.onShow) }
    val shown = found?.copy(lift = actions.lift?.takeIf { it.entranceId == found.entrance.id })
    BackHandler(enabled = shown != null, onBack = exits.onBack)
    if (shown != null) {
        EntranceSheet(shown = shown, exits = exits, actions = actions, modifier = modifier, isFull = isFull)
    }
}

/**
 * Czy arkusz wejścia [id] zajmuje cały ekran: przy skali czcionki co najmniej 1,5, w oknie szerokim na co najmniej
 * 600 dp i gdy [focus] zaznacza to wejście z ekranu budynku otwartego od razu na cały ekran.
 *
 * Wymagania: NFR-19, NFR-20, NFR-51
 */
@Composable
@ReadOnlyComposable
internal fun isFullEntranceSheet(id: String, focus: MapFocus?): Boolean {
    val density = LocalDensity.current
    val window = LocalWindowInfo.current.containerSize
    val width = with(density) { window.width.toDp() }
    val isFromFullCard = (focus as? MapFocus.Entrance)?.let { it.isFullSheet && it.id == id } == true
    return density.fontScale >= LARGE_FONT_SCALE || width >= WIDE_WINDOW || isFromFullCard
}

/**
 * Arkusz wejścia [shown] na dole mapy V1, ten sam przy każdym wejściu: znak oznaczenia przy nagłówku, wiersz budynku,
 * słowo oznaczenia, „Zalecane.” przy wejściu zalecanym, dzień ze źródłem, nazwa, dojście, drzwi, winda i bariery
 * z danych oraz zgłoszenie bariery. Nagłówek z fokusem i wiersz budynku stoją nad treścią, która się przewija. Arkusz na mapie
 * ma najwyżej połowę wysokości okna.
 *
 * Wymagania: US2.1, US2.2, US2.8, US3.3, US4.6, US17.1, US17.3, BR-25, BR-27, NFR-18, NFR-20, NFR-21, NFR-53
 *
 * @param shown wejście z katalogu z jego miejscem wśród wejść budynku
 * @param exits „Zamknij” i wiersz budynku
 * @param actions „Zgłoś barierę przy wejściu”
 * @param modifier położenie arkusza na mapie
 * @param isFull arkusz na cały ekran, bez mapy
 */
@Composable
internal fun EntranceSheet(
    shown: ShownEntrance,
    exits: SheetExits,
    actions: EntranceActions,
    modifier: Modifier = Modifier,
    isFull: Boolean = false,
) {
    val entrance = shown.entrance
    val title = entrance.name ?: stringResource(R.string.karta_bez_nazwy)
    val header: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
            MapSheetHeader(title = stringResource(R.string.mapa_wejscie_tytul), onClose = exits.onClose) {
                MarkingSign(entrance.marking)
            }
            BuildingLink(entrance.buildingSymbol) { exits.onBuilding(entrance.buildingSymbol) }
        }
    }
    val bounded = if (isFull) modifier else modifier.heightIn(max = halfWindow())
    MapSheet(modifier = bounded, isFull = isFull, header = header) {
        Text(
            text = markingLine(entrance),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = Typografia.wyroznienieWaga,
        )
        sourceLine(shown)?.let { SecondaryText(it) }
        entrance.name?.let { Text(text = it, style = MaterialTheme.typography.bodyLarge) }
        if (shown.hasDetails()) EntranceDetails(shown)
        if (entrance.marking == EntranceMarking.NO_DATA) {
            SecondaryText(stringResource(R.string.mapa_wejscie_niesprawdzone))
        }
        if (shown.barriers.isNotEmpty()) EntranceBarriers(shown)
        SheetButton(
            text = stringResource(R.string.karta_zglos),
            spoken = stringResource(R.string.karta_zglos_czytaj, title),
            onClick = { actions.onReport(entrance.id) },
        )
    }
}

@Composable
@ReadOnlyComposable
private fun halfWindow(): Dp {
    val height = LocalWindowInfo.current.containerSize.height
    return with(LocalDensity.current) { height.toDp() } / 2
}

@Composable
private fun SheetButton(text: String, spoken: String, onClick: () -> Unit) {
    PinnedButton(
        text = text,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = spoken },
        kind = PinnedButtonKind.Outlined,
    )
}

@Composable
private fun SecondaryText(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
@ReadOnlyComposable
private fun sourceLine(shown: ShownEntrance): String? {
    val recommended = stringResource(R.string.mapa_wejscie_zalecane)
    val source = shown.entrance.assessment?.let { assessmentText(it) }
    val parts = listOfNotNull(recommended.takeIf { shown.isRecommended }, source)
    return parts.takeIf { it.isNotEmpty() }?.joinToString(separator = " ")
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
