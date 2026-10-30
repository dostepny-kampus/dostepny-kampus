/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Zaokraglenia
import io.github.dostepnykampus.core.geo.searchBuildings
import io.github.dostepnykampus.core.model.BuildingCatalog

private const val ICON_VIEWPORT = 24f

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
 * Góra V1: nagłówek „Mapa kampusu”, pole symbolu budynku i wynik zaraz pod polem (US1.1). Pole przyjmuje zapis
 * „c13” i szuka bez sieci w katalogu z aplikacji. Wynik to ten sam przycisk co w arkuszu „Który budynek?”:
 * symbol, nazwa z danych i „Budynek C-13”. Tekst, który nie jest symbolem budynku, np. „A-1 329”, daje stan
 * „brak wyniku” (decyzja 82). Komunikat stoi pod polem, więc osoba widzi go tam, gdzie pisała (WCAG 3.3.1),
 * a TalkBack czyta go sam.
 *
 * @param catalog katalog budynków; `null`, dopóki się wczytuje
 * @param isOnline czy jest internet; bez niego pod nagłówkiem stoi [OfflineLine]
 * @param onBuildingClick symbol wybranego wyniku; ekran otwiera V2 i zaznacza budynek na mapie
 * @param modifier położenie nad mapą
 */
@Composable
internal fun CampusSearch(
    catalog: BuildingCatalog?,
    isOnline: Boolean,
    onBuildingClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        SearchHeader(catalog, onBuildingClick)
        if (!isOnline) OfflineLine()
    }
}

// Nagłówek, pole i wynik albo karta brak wyniku na tle powierzchni ekranu.
@Composable
private fun SearchHeader(catalog: BuildingCatalog?, onBuildingClick: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(query, catalog) { catalog?.let { searchBuildings(query, it.buildings) }.orEmpty() }
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
            SymbolField(query, onQueryChange = { query = it })
            results.forEach { building ->
                BuildingChoice(building) {
                    query = ""
                    onBuildingClick(building.symbol)
                }
            }
            if (query.isNotBlank() && catalog != null && results.isEmpty()) NoResult(query)
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
