/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.overlay

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Wysokosci
import io.github.dostepnykampus.feature.map.R
import io.github.dostepnykampus.feature.map.area.AreaList
import io.github.dostepnykampus.feature.map.area.AreaNotice
import io.github.dostepnykampus.feature.map.area.MapAreaSwitch
import io.github.dostepnykampus.feature.map.area.MapAreas
import io.github.dostepnykampus.feature.map.camera.MapCamera
import java.time.LocalDate

private const val SHORT_ATTRIBUTION_FONT_SCALE = 1.5f

/** Panel otwarty na mapie V1. Naraz otwarty jest najwyżej jeden, więc stanem nakładki jest jedna wartość. */
private enum class MapPanel { Area, Legend, Info }

/**
 * Nakładka mapy V1: przyciski powiększania i przesuwania, przycisk terenu nad „Legendą”, „ⓘ”, podpis OSM, po zmianie
 * terenu napis „Pokazuję teren: …” i nad nimi [content]. Otwarty panel zasłania przyciski, które by na nim leżały;
 * dotknięcie mapy obok panelu terenu zamyka go. Otwarty panel jest stanem zapisywanym. Nad małą kartą V2 zostają
 * tylko przyciski powiększania i przesuwania, podpis OSM i napis o zmianie terenu tuż nad kartą.
 *
 * Wymagania: NFR-53, US1.6, US1.7, NFR-37, US1.8, US2.8
 *
 * @param camera widok kamery, od którego liczą się przyciski mapy
 * @param hasControls czy mapa ma przyciski powiększania i przesuwania; nie ma ich przy otwartej klawiaturze
 *  ani nad arkuszem V2, gdy wolnej mapy jest mniej niż 300 dp
 * @param dataDay dzień danych pokazany w panelu „ⓘ”
 * @param onCameraChange widok kamery po dotknięciu przycisku mapy
 * @param areas tereny mapy: lista do panelu „Teren na mapie” i wybór terenu
 * @param onAreaShow widok startowy terenu wybranego w panelu
 * @param modifier wymiary nakładki, zwykle cała mapa bez części zasłoniętej
 * @param hasPanels czy mapa ma przycisk terenu, „Legendę” i „ⓘ”; nie ma ich nad arkuszem V2
 * @param hasNotice czy mapa może pokazać napis o zmianie terenu; nie przy otwartej klawiaturze
 * @param side elementy przy prawej krawędzi pod przyciskami mapy, a bez przycisków u góry, np. kolumna pięter
 * @param content elementy nad nakładką
 */
@Composable
internal fun MapOverlay(
    camera: MapCamera,
    hasControls: Boolean,
    dataDay: LocalDate?,
    onCameraChange: (MapCamera) -> Unit,
    areas: MapAreas,
    onAreaShow: (MapCamera) -> Unit,
    modifier: Modifier = Modifier,
    hasPanels: Boolean = hasControls,
    hasNotice: Boolean = hasPanels,
    side: @Composable ColumnScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    var saved by rememberSaveable { mutableStateOf<MapPanel?>(null) }
    val open = saved.takeIf { hasPanels }
    Box(modifier) {
        Column(modifier = Modifier.align(Alignment.TopEnd), horizontalAlignment = Alignment.End) {
            if (hasControls && open == null) {
                MapControls(
                    camera = camera,
                    onCameraChange = { moved ->
                        areas.acknowledge()
                        onCameraChange(moved)
                    },
                )
            }
            side()
        }
        if (open == MapPanel.Area) {
            Box(Modifier.matchParentSize().pointerInput(Unit) { detectTapGestures { saved = null } })
        }
        if (hasPanels && open != MapPanel.Info) {
            val above = Modifier.align(Alignment.TopStart).padding(bottom = MapControlTouchSize + Odstepy.s)
            AreaAndLegendColumn(
                areas = areas,
                open = open,
                onOpenChange = { saved = it },
                onAreaShow = onAreaShow,
                modifier = above,
            )
        }
        Column(Modifier.align(Alignment.BottomStart)) {
            areas.notice?.takeIf { hasNotice && open == null }?.let {
                AreaNotice(shift = it, isNew = areas.isNoticeNew, onShow = areas::noticeShown)
            }
            OsmAttribution()
        }
        if (hasPanels) {
            MapInfo(
                isOpen = open == MapPanel.Info,
                dataDay = dataDay,
                onOpenChange = { saved = MapPanel.Info.takeIf { _ -> it } },
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
        content()
    }
}

@Composable
private fun AreaAndLegendColumn(
    areas: MapAreas,
    open: MapPanel?,
    onOpenChange: (MapPanel?) -> Unit,
    onAreaShow: (MapCamera) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        MapAreaSwitch(
            areas = AreaList(areas.choices),
            isOpen = open == MapPanel.Area,
            onOpenChange = { onOpenChange(MapPanel.Area.takeIf { _ -> it }) },
            onChoose = { name ->
                areas.show(name)?.let(onAreaShow)
                onOpenChange(null)
            },
            modifier =
                Modifier
                    .weight(weight = 1f, fill = false)
                    .padding(start = Odstepy.s, top = Odstepy.s, end = Odstepy.s),
        )
        if (open != MapPanel.Area) {
            MapLegend(
                isOpen = open == MapPanel.Legend,
                onOpenChange = { onOpenChange(MapPanel.Legend.takeIf { _ -> it }) },
            )
        }
    }
}

@Composable
private fun OsmAttribution(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val isShort = LocalDensity.current.fontScale >= SHORT_ATTRIBUTION_FONT_SCALE
    Box(
        modifier =
            modifier
                .padding(horizontal = Odstepy.s)
                .clickable { uriHandler.openUri(OSM_COPYRIGHT) }
                .heightIn(min = MapControlTouchSize),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            shadowElevation = Wysokosci.kontrolkiMapy,
            border = mapControlBorder(),
        ) {
            Text(
                text = stringResource(if (isShort) R.string.mapa_osm_krotko else R.string.mapa_osm),
                modifier = Modifier.padding(horizontal = Odstepy.s),
                maxLines = 1,
                textDecoration = TextDecoration.Underline,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
