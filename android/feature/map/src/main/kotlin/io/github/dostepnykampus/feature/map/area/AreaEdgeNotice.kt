/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.area

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.feature.map.R
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.camera.MapLimits
import io.github.dostepnykampus.feature.map.campus.CampusBounds

private const val EDGE_TOLERANCE_DEGREES = 1e-6

/**
 * Czy środek widoku leży na brzegu prostokąta [bounds], w którym może leżeć środek widoku: tam strzałka albo gest
 * nie przesuwają mapy dalej, bo brzeg mapy stoi na brzegu terenu.
 *
 * Wymagania: US1.7, NFR-53
 */
internal fun MapCamera.isAtEdgeOf(bounds: CampusBounds): Boolean =
    center.longitude.isOnEdge(low = bounds.west, high = bounds.east) ||
        center.latitude.isOnEdge(low = bounds.south, high = bounds.north)

private fun Double.isOnEdge(low: Double, high: Double): Boolean =
    this <= low + EDGE_TOLERANCE_DEGREES || this >= high - EDGE_TOLERANCE_DEGREES

/**
 * Nazwa terenu w komunikacie o jego końcu: [area], gdy widok jest na brzegu ([atEdge]), inaczej `null`. Region
 * dynamiczny TalkBack czyta tekst przy każdej jego zmianie, więc komunikat pada raz przy dojściu do brzegu,
 * ponownie dopiero po odsunięciu widoku od brzegu albo po zmianie terenu.
 *
 * Wymagania: US1.7, NFR-19
 */
internal fun areaEdgeName(area: String?, atEdge: Boolean): String? = area.takeIf { atEdge }

/**
 * Niewidoczne ogłoszenie dla TalkBack: „Tu kończy się teren Kampus główny. Inny teren wybierzesz przyciskiem
 * «Zmień teren» u góry mapy.” Nic nie rysuje; ogłasza, gdy brzeg widoku [camera] dochodzi do brzegu terenu [area]
 * w granicach [limits], zob. [areaEdgeName].
 *
 * Wymagania: US1.7, NFR-19
 */
@Composable
internal fun AreaEdgeAnnouncement(camera: MapCamera, limits: MapLimits, area: String?, modifier: Modifier = Modifier) {
    val name = areaEdgeName(area, atEdge = camera.isAtEdgeOf(limits.centerBounds(zoom = camera.zoom)))
    val text = name?.let { stringResource(R.string.mapa_teren_granica, it) }.orEmpty()
    Box(
        modifier.size(1.dp).semantics {
            liveRegion = LiveRegionMode.Polite
            contentDescription = text
        },
    )
}
