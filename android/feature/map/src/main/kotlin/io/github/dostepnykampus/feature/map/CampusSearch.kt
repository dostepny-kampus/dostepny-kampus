/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.core.designsystem.Zaokraglenia
import io.github.dostepnykampus.core.geo.searchBuildings
import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.BuildingCatalog

private const val ICON_VIEWPORT = 24f

/** Skala czcionki, od której przełącznik ma krótki napis „Lista” (NFR-20). */
internal const val LIST_FONT_SCALE = 1.5f

// Lupa i „×” z Material Icons, siatka 24 × 24, jak ikony paska dolnego.
private val SearchIcon =
    icon(
        "M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16" +
            "c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 " +
            "9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z",
    )
private val ClearIcon =
    icon("M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z")

/**
 * Góra V1: nagłówek „Mapa kampusu”, pole symbolu budynku, przełącznik „Mapa” i „Lista budynków” (NFR-19) i wynik
 * pod nimi (US1.1). Pole przyjmuje zapis „c13” i szuka bez sieci w katalogu z aplikacji. Wynik to ten sam przycisk
 * co w arkuszu „Który budynek?”: symbol, nazwa z danych i „Budynek C-13”. Tekst, który nie jest symbolem budynku,
 * np. „A-1 329”, daje stan „brak wyniku” (decyzja 82). Komunikat stoi pod polem, więc osoba widzi go tam, gdzie
 * pisała (WCAG 3.3.1), a TalkBack czyta go sam. Przy liście pole zawęża listę zamiast pokazywać wynik.
 *
 * @param catalog katalog budynków; `null`, dopóki się wczytuje
 * @param isOnline czy jest internet; bez niego pod przełącznikiem stoi [OfflineLine]
 * @param isList czy przełącznik stoi na „Lista budynków”; wtedy pod górą jest lista budynków
 * @param onListChange wybór w przełączniku
 * @param onBuildingClick symbol wybranego wyniku albo budynku z listy; ekran otwiera V2 i zaznacza budynek
 * @param modifier położenie nad mapą; przy liście cała wysokość nad paskiem dolnym
 */
@Composable
internal fun CampusSearch(
    catalog: BuildingCatalog?,
    isOnline: Boolean,
    isList: Boolean,
    onListChange: (Boolean) -> Unit,
    onBuildingClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(query, catalog) { catalog?.let { searchBuildings(query, it.buildings) }.orEmpty() }
    val isNoResult = query.isNotBlank() && catalog != null && results.isEmpty()
    // Przy liście wynik to sama lista, więc pod polem zostaje tylko karta brak wyniku.
    val shown = results.takeUnless { isList }.orEmpty()
    Column(modifier.fillMaxWidth()) {
        SearchHeader(query = query, onQueryChange = { query = it }, isList = isList, onListChange = onListChange)
        if (!isOnline) OfflineLine()
        if (isNoResult || shown.isNotEmpty()) {
            ResultPanel {
                shown.forEach { building ->
                    BuildingChoice(building) {
                        query = ""
                        onBuildingClick(building.symbol)
                    }
                }
                query.takeIf { isNoResult }?.let { NoResult(it) }
            }
        }
        if (isList) {
            BuildingList(
                catalog = catalog,
                query = query,
                onBuildingClick = onBuildingClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// Nagłówek, pole i przełącznik na tle powierzchni ekranu.
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

// „Mapa” i „Lista budynków”; wybraną pozycję widać po tle i ptaszku, nie tylko po kolorze (NFR-19, NFR-21).
// Przy dużej czcionce widać „Lista”, a TalkBack dalej czyta „Lista budynków” (NFR-20).
@Composable
private fun ViewToggle(isList: Boolean, onListChange: (Boolean) -> Unit) {
    val isShort = LocalDensity.current.fontScale >= LIST_FONT_SCALE
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

// Wynik albo karta brak wyniku pod przełącznikiem i linią „Bez internetu”.
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

// Budynki w kolejności katalogu, jak wynik pola; wpisany symbol zawęża listę (NFR-19, US2.5).
@Composable
private fun BuildingList(
    catalog: BuildingCatalog?,
    query: String,
    onBuildingClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val buildings =
        remember(query, catalog) {
            catalog?.run { if (query.isBlank()) buildings else searchBuildings(query, buildings) }.orEmpty()
        }
    val insets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
    LazyColumn(
        modifier = modifier.fillMaxWidth().windowInsetsPadding(insets),
        contentPadding = PaddingValues(Odstepy.m),
        verticalArrangement = Arrangement.spacedBy(Odstepy.s),
    ) {
        items(buildings, key = Building::symbol) { building ->
            BuildingChoice(building) { onBuildingClick(building.symbol) }
        }
    }
}

// Pole w kształcie kapsuły z lupą; „×” z nazwą „Wyczyść pole” pojawia się po wpisaniu tekstu (NFR-53, NFR-18).
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

// „Nie znaleźliśmy budynku »X«.” z radą, w białym kontenerze pod polem (UC1 3a, PW-9).
@Composable
private fun NoResult(query: String) {
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
        }
    }
}

// Ikona z jednej ścieżki Material Icons na siatce 24 × 24.
internal fun icon(path: String): ImageVector {
    val builder =
        ImageVector.Builder(
            defaultWidth = ICON_VIEWPORT.dp,
            defaultHeight = ICON_VIEWPORT.dp,
            viewportWidth = ICON_VIEWPORT,
            viewportHeight = ICON_VIEWPORT,
        )
    builder.addPath(pathData = addPathNodes(path), fill = SolidColor(Color.Black))
    return builder.build()
}
