/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.area

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.feature.map.R
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.campus.CampusBounds

private const val EDGE_TOLERANCE_DEGREES = 1e-6

/**
 * Czy środek widoku leży na brzegu prostokąta terenu [bounds]: tam strzałka albo gest nie przesuwają mapy dalej.
 *
 * Wymagania: US1.7, NFR-53
 */
internal fun MapCamera.isAtEdgeOf(bounds: CampusBounds): Boolean =
    center.longitude.isOnEdge(low = bounds.west, high = bounds.east) ||
        center.latitude.isOnEdge(low = bounds.south, high = bounds.north)

private fun Double.isOnEdge(low: Double, high: Double): Boolean =
    this <= low + EDGE_TOLERANCE_DEGREES || this >= high - EDGE_TOLERANCE_DEGREES

/**
 * Pilnuje, by komunikat o końcu terenu padł raz: [isDue] zwraca prawdę przy dojściu do brzegu, a powtórzenie jest
 * możliwe dopiero po odsunięciu widoku od brzegu albo po zmianie terenu.
 *
 * Wymagania: US1.7, NFR-19
 */
internal class AreaEdgeNotice {
    private var area: String? = null
    private var isAtEdge = false

    /** Czy przy widoku na brzegu ([atEdge]) terenu [area] trzeba teraz ogłosić koniec terenu. */
    fun isDue(area: String?, atEdge: Boolean): Boolean {
        val due = area != null && atEdge && (!isAtEdge || area != this.area)
        this.area = area
        isAtEdge = atEdge
        return due
    }
}

/**
 * Niewidoczne ogłoszenie dla TalkBack: „Tu kończy się teren Kampus główny. Inny teren wybierzesz przyciskiem
 * «Zmień teren» u góry mapy.” Nic nie rysuje; ogłasza raz przy dojściu widoku [camera] do brzegu [bounds] terenu
 * [area], zob. [AreaEdgeNotice].
 *
 * Wymagania: US1.7, NFR-19
 */
@Composable
internal fun AreaEdgeAnnouncement(
    camera: MapCamera,
    bounds: CampusBounds,
    area: String?,
    modifier: Modifier = Modifier,
) {
    val notice = remember { AreaEdgeNotice() }
    var isDue by remember { mutableStateOf(value = false) }
    LaunchedEffect(camera, bounds, area) { isDue = notice.isDue(area, atEdge = camera.isAtEdgeOf(bounds)) }
    val text = if (isDue && area != null) stringResource(R.string.mapa_teren_granica, area) else ""
    Box(
        modifier.size(1.dp).semantics {
            liveRegion = LiveRegionMode.Polite
            contentDescription = text
        },
    )
}
