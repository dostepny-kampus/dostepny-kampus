/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.overlay

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Wysokosci
import io.github.dostepnykampus.core.designsystem.Zaokraglenia
import io.github.dostepnykampus.core.designsystem.campusDay
import io.github.dostepnykampus.core.model.building.AssessmentSource
import io.github.dostepnykampus.core.model.building.BuildingCatalog
import io.github.dostepnykampus.feature.map.R
import java.time.LocalDate

/** Strona praw autorskich OpenStreetMap, cel odnośnika w podpisie mapy i w panelu „Źródła danych”. Wymagania: NFR-37 */
internal const val OSM_COPYRIGHT = "https://www.openstreetmap.org/copyright"

/** Strona projektu OpenFreeMap, dostawcy kafelków podkładu (ADR-0014). */
internal const val OPENFREEMAP_SITE = "https://openfreemap.org/"

/** Strona schematu i licencji OpenMapTiles, w którym są kafelki podkładu (ADR-0014). */
internal const val OPENMAPTILES_SITE = "https://openmaptiles.org/"

/** Dzień stanu danych z importu: najpóźniejszy dzień oznaczenia wejść ze źródła PWr; `null`, gdy takich nie ma. */
internal fun BuildingCatalog.dataDay(): LocalDate? {
    val imported = entrances.mapNotNull { it.assessment }.filter { it.source == AssessmentSource.IMPORT }
    return imported.maxOfOrNull { it.date }
}

/**
 * Przycisk „ⓘ” mapy V1, a nad nim, gdy [isOpen], arkusz legendy: znaki mapy, a po nich źródła danych z dniem
 * stanu danych PWr. Systemowe „Wstecz” przy otwartym arkuszu zamyka go i zostawia mapę.
 *
 * Wymagania: US1.4, NFR-21, NFR-37, NFR-40, NFR-17, NFR-18
 *
 * @param isOpen czy panel jest otwarty
 * @param dataDay dzień stanu danych PWr albo `null`, gdy dane go nie podają
 * @param onOpenChange dotknięcie przycisku albo „Zamknij” w panelu
 * @param modifier położenie na mapie
 */
@Composable
internal fun MapInfo(
    isOpen: Boolean,
    dataDay: LocalDate?,
    onOpenChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = isOpen) { onOpenChange(false) }
    Column(modifier.padding(horizontal = Odstepy.s), horizontalAlignment = Alignment.End) {
        if (isOpen) {
            val panel = Modifier.weight(weight = 1f, fill = false).padding(bottom = Odstepy.s)
            InfoPanel(dataDay = dataDay, onClose = { onOpenChange(false) }, modifier = panel)
        }
        MapButton(name = R.string.mapa_informacje, icon = MapControlIcons.Info, isActive = isOpen) {
            onOpenChange(!isOpen)
        }
    }
}

@Composable
private fun InfoPanel(dataDay: LocalDate?, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Zaokraglenia.duze),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = Wysokosci.kontrolkiMapy,
        border = mapControlBorder(),
    ) {
        val scroll = rememberScrollState()
        Column {
            InfoHeader(onClose)
            HorizontalDivider()
            val inner = Modifier.weight(weight = 1f, fill = false).verticalScroll(scroll).padding(Odstepy.m)
            Column(inner, verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
                LegendSigns()
                HorizontalDivider()
                Text(
                    text = stringResource(R.string.mapa_zrodla),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleSmall,
                )
                StreetSource()
                BasemapSource()
                val day = dataDay?.let { campusDay(it) }
                val campus =
                    if (day == null) {
                        stringResource(R.string.mapa_zrodla_pwr)
                    } else {
                        stringResource(R.string.mapa_zrodla_pwr_stan, day)
                    }
                Text(text = campus, style = MaterialTheme.typography.bodyLarge)
            }
            if (scroll.canScrollForward) ScrollHint()
        }
    }
}

@Composable
private fun InfoHeader(onClose: () -> Unit) {
    Row(Modifier.padding(start = Odstepy.m, end = Odstepy.s), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.mapa_legenda),
            modifier = Modifier.weight(1f).semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
        )
        OutlinedButton(onClick = onClose, modifier = Modifier.heightIn(min = CeleDotyku.minimum)) {
            Text(stringResource(R.string.mapa_zamknij))
        }
    }
}

@Composable
private fun BasemapSource() {
    val link = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)
    val provider = stringResource(R.string.mapa_openfreemap)
    val schema = stringResource(R.string.mapa_openmaptiles)
    val line = stringResource(R.string.mapa_zrodla_podklad, provider, schema)
    val text =
        buildAnnotatedString {
            append(line)
            listOf(provider to OPENFREEMAP_SITE, schema to OPENMAPTILES_SITE).forEach { (name, site) ->
                val start = line.indexOf(name)
                addLink(LinkAnnotation.Url(site, TextLinkStyles(link)), start = start, end = start + name.length)
            }
        }
    Text(text = text, style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun StreetSource() {
    val link = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)
    val before = stringResource(R.string.mapa_zrodla_osm_przed)
    val name = stringResource(R.string.mapa_osm)
    val after = stringResource(R.string.mapa_zrodla_osm_po)
    val text =
        buildAnnotatedString {
            append(before)
            withLink(LinkAnnotation.Url(OSM_COPYRIGHT, TextLinkStyles(link))) { append(name) }
            append(after)
        }
    Text(text = text, style = MaterialTheme.typography.bodyLarge)
}
