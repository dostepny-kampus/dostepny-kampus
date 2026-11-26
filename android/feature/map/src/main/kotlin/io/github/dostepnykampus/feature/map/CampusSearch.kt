/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.LARGE_FONT_SCALE
import io.github.dostepnykampus.core.designsystem.Obramowania
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.OfflineLine
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.core.designsystem.Zaokraglenia
import io.github.dostepnykampus.core.designsystem.bar.NavigationIcons
import io.github.dostepnykampus.core.designsystem.pathIcon
import io.github.dostepnykampus.core.model.building.BuildingCatalog
import io.github.dostepnykampus.core.model.room.FoundRoom
import io.github.dostepnykampus.feature.map.area.AreaOrder
import io.github.dostepnykampus.feature.map.building.BuildingList
import io.github.dostepnykampus.feature.map.building.BuildingSearch
import io.github.dostepnykampus.feature.map.search.SearchResults
import kotlinx.coroutines.flow.drop
import java.time.Instant

private val SearchIcon =
    pathIcon(
        "M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16" +
            "c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 " +
            "9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z",
    )
private val ListIcon =
    pathIcon("M3 13h2v-2H3v2zm0 4h2v-2H3v2zm0-8h2V7H3v2zm4 4h14v-2H7v2zm0 4h14v-2H7v2zM7 7v2h14V7H7z")
private val ClearIcon =
    pathIcon("M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z")

/**
 * Góra V1: wiersz tytułu z przejściem między mapą a listą, pole symbolu budynku i wynik pod nimi. Pole przyjmuje
 * zapis „c13” i szuka bez sieci; tekst, który nie jest symbolem, daje kartę „brak wyniku”, która przy mapie
 * prowadzi do listy. Przy liście pole zawęża listę.
 *
 * Wymagania: NFR-19, US1.1, UC1, NFR-21, NFR-20, NFR-53, NFR-18, US2.5, NFR-11, US1.8, NFR-15
 *
 * @param catalog katalog budynków; `null`, dopóki się wczytuje
 * @param search wynik wyszukiwania z modelu widoku; ekran pokazuje go tylko dla zapytania z pola
 * @param onSearch nowe zapytanie z pola, do policzenia w modelu widoku
 * @param isOnline czy jest internet; bez niego pod polem stoi [OfflineLine]
 * @param syncedAt chwila pobrania najstarszych danych z serwera do daty w [OfflineLine]
 * @param isList czy widok to lista; wtedy pod górą jest lista budynków
 * @param areas tereny w kolejności nagłówków listy budynków
 * @param onListChange wybór widoku w wierszu tytułu albo przejście do listy z karty „brak wyniku”
 * @param onBuildingClick symbol wybranego wyniku albo budynku z listy; ekran otwiera V2 i zaznacza budynek. Wybór
 *   zdejmuje fokus z pola, chowa klawiaturę ekranową i wyniki, a tekst w polu zostaje; utrata fokusu z innego
 *   powodu też chowa wyniki. Wyniki wracają po dotknięciu pola albo zmianie tekstu. Pierwsze „Wstecz” zamyka V2
 * @param modifier położenie nad mapą; przy liście cała wysokość nad paskiem dolnym
 * @param onRoomClick wybrany wynik sali; wybór chowa wyniki i klawiaturę jak wybór budynku
 * @param onBarHeight wysokość w pikselach wiersza tytułu, pola i wiersza bez internetu, bez wyników: tę część
 *   mapy kamera traktuje jako zasłoniętą, więc wpisywanie nie przesuwa mapy
 * @param searchFocus fokus pola widziany przez mapę i listę
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
    onRoomClick: (FoundRoom) -> Unit = {},
    onBarHeight: (Int) -> Unit = {},
    searchFocus: SearchFocus? = null,
) {
    val field = rememberTextFieldState()
    val query = field.text.toString()
    var isHidden by rememberSaveable { mutableStateOf(false) }
    SideEffect { onSearch(query) }
    LaunchedEffect(field) { snapshotFlow { field.text.toString() }.drop(1).collect { isHidden = false } }
    val showList = {
        field.clearText()
        onListChange(true)
    }
    val release = searchRelease { isHidden = true }
    val choose = { symbol: String ->
        release()
        onBuildingClick(symbol)
    }
    Column(modifier.fillMaxWidth()) {
        Column(
            Modifier
                .onSizeChanged { onBarHeight(it.height) }
                .onFocusChanged { field -> searchFocus?.onField(isFocused = field.hasFocus) },
        ) {
            SearchHeader(
                field = field,
                onFieldTap = { isHidden = false },
                onFieldLeave = { isHidden = true },
                isList = isList,
                onListChange = onListChange,
            )
            if (!isOnline) OfflineLine(syncedAt)
        }
        SearchResults(
            search = search,
            query = query,
            isList = isList,
            isHidden = isHidden,
            areas = areas,
            onBuildingClick = choose,
            onRoomClick = onRoomClick,
            onRelease = release,
            onShowList = showList,
        )
        if (isList) {
            BuildingList(
                catalog = catalog,
                query = query,
                areas = areas,
                search = search,
                onBuildingClick = choose,
                modifier = Modifier.weight(1f).releasesSearchOnBlankTap(searchFocus),
            )
        }
    }
}

@Composable
@ReadOnlyComposable
private fun searchRelease(onHide: () -> Unit): () -> Unit {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    return {
        onHide()
        focus.clearFocus(force = true)
        keyboard?.hide()
    }
}

@Composable
private fun SearchHeader(
    field: TextFieldState,
    onFieldTap: () -> Unit,
    onFieldLeave: () -> Unit,
    isList: Boolean,
    onListChange: (Boolean) -> Unit,
) {
    val insets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
        Column {
            Column(
                Modifier.windowInsetsPadding(insets).padding(horizontal = Odstepy.m, vertical = Odstepy.s),
                verticalArrangement = Arrangement.spacedBy(Odstepy.s),
            ) {
                TitleRow(isList, onListChange)
                SymbolField(state = field, onTap = onFieldTap, onLeave = onFieldLeave)
            }
            HorizontalDivider(thickness = Obramowania.ramka, color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun TitleRow(isList: Boolean, onListChange: (Boolean) -> Unit) {
    val title: @Composable () -> Unit = {
        Text(
            text = stringResource(R.string.mapa_kampusu),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
        )
    }
    if (LocalDensity.current.fontScale >= LARGE_FONT_SCALE) {
        Column {
            title()
            ViewButton(isList = isList, hasIcon = false, onListChange = onListChange)
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { title() }
            ViewButton(isList = isList, hasIcon = true, onListChange = onListChange)
        }
    }
}

@Composable
private fun ViewButton(isList: Boolean, hasIcon: Boolean, onListChange: (Boolean) -> Unit) {
    TextButton(onClick = { onListChange(!isList) }, modifier = Modifier.heightIn(min = CeleDotyku.minimum)) {
        if (hasIcon) {
            Icon(imageVector = if (isList) NavigationIcons.Map else ListIcon, contentDescription = null)
            Spacer(Modifier.width(Odstepy.s))
        }
        Text(
            text = stringResource(if (isList) R.string.mapa_pokaz_widok_mapy else R.string.mapa_pokaz_liste),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = Typografia.wyroznienieWaga,
        )
    }
}

@Composable
private fun SymbolField(state: TextFieldState, onTap: () -> Unit, onLeave: () -> Unit) {
    val interactions = remember { MutableInteractionSource() }
    val tap by rememberUpdatedState(onTap)
    LaunchedEffect(interactions) {
        interactions.interactions.collect { if (it is PressInteraction.Release) tap() }
    }
    val scheme = MaterialTheme.colorScheme
    val colors =
        OutlinedTextFieldDefaults.colors(
            focusedContainerColor = scheme.surfaceContainerLowest,
            unfocusedContainerColor = scheme.surfaceContainerLowest,
            unfocusedBorderColor = scheme.outline,
        )
    val textStyle = LocalTextStyle.current.merge(TextStyle(color = scheme.onSurface))
    val material =
        OutlinedTextFieldDefaults.decorator(
            state = state,
            enabled = true,
            lineLimits = TextFieldLineLimits.SingleLine,
            outputTransformation = null,
            interactionSource = interactions,
            leadingIcon = { Icon(SearchIcon, contentDescription = null) },
            trailingIcon = { if (state.text.isNotEmpty()) ClearButton { state.clearText() } },
            colors = colors,
            container = {
                OutlinedTextFieldDefaults.Container(
                    enabled = true,
                    isError = false,
                    interactionSource = interactions,
                    colors = colors,
                    shape = RoundedCornerShape(Zaokraglenia.kapsula),
                    focusedBorderThickness = Obramowania.pole,
                    unfocusedBorderThickness = Obramowania.pole,
                )
            },
        )
    BasicTextField(
        state = state,
        modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = OutlinedTextFieldDefaults.MinHeight)
                .onFocusChanged { if (!it.isFocused) onLeave() },
        textStyle = textStyle,
        keyboardOptions =
            KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Search),
        lineLimits = TextFieldLineLimits.SingleLine,
        interactionSource = interactions,
        cursorBrush = SolidColor(scheme.primary),
        decorator =
            TextFieldDecorator { inner ->
                material.Decoration {
                    FieldWithHint(isEmpty = state.text.isEmpty(), style = textStyle, content = inner)
                }
            },
    )
}

@Composable
private fun FieldWithHint(isEmpty: Boolean, style: TextStyle, content: @Composable () -> Unit) {
    val isWrapped = isEmpty && LocalDensity.current.fontScale >= LARGE_FONT_SCALE
    Box(contentAlignment = Alignment.CenterStart, propagateMinConstraints = true) {
        Text(
            text = stringResource(R.string.mapa_szukaj_podpowiedz),
            modifier = if (isEmpty) Modifier else Modifier.alpha(0f).clearAndSetSemantics {},
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (isWrapped) Int.MAX_VALUE else 1,
            overflow = TextOverflow.Ellipsis,
            style = style,
        )
        content()
    }
}

@Composable
private fun ClearButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(ClearIcon, contentDescription = stringResource(R.string.mapa_wyczysc_pole))
    }
}
