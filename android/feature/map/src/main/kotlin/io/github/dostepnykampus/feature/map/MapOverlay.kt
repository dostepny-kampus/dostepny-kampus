/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Wysokosci
import java.time.LocalDate

private const val SHORT_ATTRIBUTION_FONT_SCALE = 1.5f

/** Panel otwarty na mapie V1. Naraz otwarty jest najwyżej jeden, więc stanem nakładki jest jedna wartość. */
private enum class MapPanel { Legend, Info }

/**
 * Nakładka mapy V1: przyciski powiększania i przesuwania (NFR-53), „Legenda”, przycisk „ⓘ” i podpis OSM (NFR-37),
 * a nad nimi [content], np. arkusz „Który budynek?”. Otwarty panel zasłania przyciski mapy, a otwarte „ⓘ” także
 * „Legendę”. Otwarty panel jest stanem zapisywanym.
 *
 * @param camera widok kamery, od którego liczą się przyciski mapy
 * @param hasControls czy mapa ma przyciski; nie ma ich pod arkuszem V2 i przy otwartej klawiaturze (decyzja 91)
 * @param dataDay dzień danych pokazany w panelu „ⓘ”
 * @param onCameraChange widok kamery po dotknięciu przycisku mapy
 * @param modifier wymiary nakładki, zwykle cała mapa bez części zasłoniętej
 * @param content elementy nad nakładką
 */
@Composable
internal fun MapOverlay(
    camera: MapCamera,
    hasControls: Boolean,
    dataDay: LocalDate?,
    onCameraChange: (MapCamera) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    var open by rememberSaveable { mutableStateOf<MapPanel?>(null) }
    Box(modifier) {
        if (hasControls && open == null) {
            MapControls(camera, onCameraChange, modifier = Modifier.align(Alignment.TopEnd))
        }
        if (hasControls && open != MapPanel.Info) {
            val above = Modifier.align(Alignment.TopStart).padding(bottom = MapControlTouchSize + Odstepy.s)
            MapLegend(
                isOpen = open == MapPanel.Legend,
                onOpenChange = { open = MapPanel.Legend.takeIf { _ -> it } },
                modifier = above,
            )
        }
        OsmAttribution(Modifier.align(Alignment.BottomStart))
        if (hasControls) {
            MapInfo(
                isOpen = open == MapPanel.Info,
                dataDay = dataDay,
                onOpenChange = { open = MapPanel.Info.takeIf { _ -> it } },
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
        content()
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
