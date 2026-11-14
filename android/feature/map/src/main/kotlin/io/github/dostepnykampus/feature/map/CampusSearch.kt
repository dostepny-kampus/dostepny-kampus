/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.LARGE_FONT_SCALE
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.core.designsystem.Zaokraglenia
import io.github.dostepnykampus.core.designsystem.pathIcon
import io.github.dostepnykampus.core.model.BuildingCatalog
import io.github.dostepnykampus.feature.map.area.AreaOrder
import io.github.dostepnykampus.feature.map.area.otherAreaOf
import io.github.dostepnykampus.feature.map.building.BuildingChoice
import io.github.dostepnykampus.feature.map.building.BuildingList
import io.github.dostepnykampus.feature.map.building.BuildingSearch
import io.github.dostepnykampus.feature.map.building.foundFor
import io.github.dostepnykampus.feature.map.controls.OfflineLine
import java.time.Instant

private val SearchIcon =
    pathIcon(
        "M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16" +
            "c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 " +
            "9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z",
    )
private val ClearIcon =
    pathIcon("M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z")

/**
 * Góra V1: nagłówek „Mapa kampusu”, pole symbolu budynku, przełącznik „Mapa” i „Lista budynków” i wynik pod nimi. Pole
 * przyjmuje zapis „c13” i szuka bez sieci; wynik to przycisk jak w arkuszu „Który budynek?”, a tekst, który nie jest
 * symbolem, daje pod polem kartę „brak wyniku” z radą i przyciskiem „Lista budynków”, którą TalkBack czyta sam. Przy
 * liście pole zawęża listę, a karta „brak wyniku” nie ma przycisku. Przełącznik pokazuje wybór tłem i ptaszkiem, przy
 * dużej czcionce jako „Lista”. Pole ma kształt kapsuły z lupą, a po wpisaniu „×” z nazwą „Wyczyść pole”.
 *
 * Wymagania: NFR-19, US1.1, UC1, NFR-21, NFR-20, NFR-53, NFR-18, US2.5, NFR-11, US1.8, NFR-15
 *
 * @param catalog katalog budynków; `null`, dopóki się wczytuje
 * @param search wynik wyszukiwania z modelu widoku; ekran pokazuje go tylko dla zapytania z pola
 * @param onSearch nowe zapytanie z pola, do policzenia w modelu widoku
 * @param isOnline czy jest internet; bez niego pod przełącznikiem stoi [OfflineLine]
 * @param syncedAt chwila pobrania najstarszych danych z serwera do daty w [OfflineLine]
 * @param isList czy przełącznik stoi na „Lista budynków”; wtedy pod górą jest lista budynków
 * @param areas tereny w kolejności nagłówków listy budynków
 * @param onListChange wybór w przełączniku albo „Lista budynków” w karcie brak wyniku
 * @param onBuildingClick symbol wybranego wyniku albo budynku z listy; ekran otwiera V2 i zaznacza budynek. Wybór
 *   zdejmuje fokus z pola i chowa klawiaturę ekranową, a pierwsze „Wstecz” zamyka V2
 * @param modifier położenie nad mapą; przy liście cała wysokość nad paskiem dolnym
 */
@Composable
internal fun CampusSearch(
    catalog: BuildingCatalog?,
    search: BuildingSearch,
    onSearch: (String) -> Unit,
    isOnline: Boolean,
    syncedAt: Instant?,
    isList: Boolean,
    areas: AreaOrder,
    onListChange: (Boolean) -> Unit,
    onBuildingClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    SideEffect { onSearch(query) }
    val found = search.foundFor(query)
    val results = found.orEmpty()
    val isNoResult = query.isNotBlank() && found?.isEmpty() == true
    val shown = results.takeUnless { isList }.orEmpty()
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val showList = {
        query = ""
        onListChange(true)
    }
    val choose = { symbol: String ->
        query = ""
        focus.clearFocus(force = true)
        keyboard?.hide()
        onBuildingClick(symbol)
    }
    Column(modifier.fillMaxWidth()) {
        SearchHeader(query = query, onQueryChange = { query = it }, isList = isList, onListChange = onListChange)
        if (!isOnline) OfflineLine(syncedAt)
        if (isNoResult || shown.isNotEmpty()) {
            ResultPanel {
                shown.forEach { building ->
                    BuildingChoice(building, areas.otherAreaOf(building.symbol)) { choose(building.symbol) }
                }
                query.takeIf { isNoResult }?.let { NoResult(it, onShowList = showList.takeUnless { isList }) }
            }
        }
        if (isList) {
            BuildingList(
                catalog = catalog,
                query = query,
                areas = areas,
                search = search,
                onBuildingClick = choose,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SearchHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    isList: Boolean,
    onListChange: (Boolean) -> Unit,
) {
    val insets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier.windowInsetsPadding(insets).padding(horizontal = Odstepy.m, vertical = Odstepy.s),
            verticalArrangement = Arrangement.spacedBy(Odstepy.s),
        ) {
            Text(
                text = stringResource(R.string.mapa_kampusu),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
            )
            SymbolField(query, onQueryChange)
            ViewToggle(isList, onListChange)
        }
    }
}

@Composable
private fun ViewToggle(isList: Boolean, onListChange: (Boolean) -> Unit) {
    val isShort = LocalDensity.current.fontScale >= LARGE_FONT_SCALE
    val colors =
        SegmentedButtonDefaults.colors(
            activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
            inactiveContainerColor = MaterialTheme.colorScheme.surface,
        )
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        listOf(false, true).forEachIndexed { index, list ->
            SegmentedButton(
                selected = isList == list,
                onClick = { onListChange(list) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                modifier = Modifier.heightIn(min = CeleDotyku.minimum),
                colors = colors,
                contentPadding = PaddingValues(horizontal = Odstepy.xs),
            ) {
                val name = stringResource(if (list) R.string.mapa_widok_lista else R.string.mapa_widok_mapa)
                Text(
                    text = if (list && isShort) stringResource(R.string.mapa_widok_lista_krotko) else name,
                    modifier = Modifier.semantics { contentDescription = name },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = Typografia.wyroznienieWaga.takeIf { isList == list },
                )
            }
        }
    }
}

@Composable
private fun ResultPanel(content: @Composable ColumnScope.() -> Unit) {
    val insets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier.windowInsetsPadding(insets).padding(horizontal = Odstepy.m, vertical = Odstepy.s),
            verticalArrangement = Arrangement.spacedBy(Odstepy.s),
            content = content,
        )
    }
}

@Composable
private fun SymbolField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.mapa_szukaj_podpowiedz)) },
        leadingIcon = { Icon(SearchIcon, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(ClearIcon, contentDescription = stringResource(R.string.mapa_wyczysc_pole))
                }
            }
        },
        keyboardOptions =
            KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Search),
        singleLine = true,
        shape = RoundedCornerShape(Zaokraglenia.kapsula),
    )
}

@Composable
private fun NoResult(query: String, onShowList: (() -> Unit)?) {
    Surface(
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(Zaokraglenia.duze),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(Modifier.padding(Odstepy.m), verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
            Text(
                text = stringResource(R.string.mapa_brak_wyniku, query.trim()),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(text = stringResource(R.string.mapa_brak_wyniku_rada), style = MaterialTheme.typography.bodyLarge)
            onShowList?.let { showList ->
                val buttonModifier = Modifier.fillMaxWidth().heightIn(min = CeleDotyku.przycisk)
                FilledTonalButton(onClick = showList, modifier = buttonModifier) {
                    Text(text = stringResource(R.string.mapa_widok_lista), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
