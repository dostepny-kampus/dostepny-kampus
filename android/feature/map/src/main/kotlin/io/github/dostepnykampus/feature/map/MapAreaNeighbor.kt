/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.core.designsystem.Wysokosci
import io.github.dostepnykampus.feature.map.data.AreaNeighbor
import io.github.dostepnykampus.feature.map.data.MapEdge

private const val QUARTER_TURN = 90f
private const val EIGHTH_TURN = 45f
private val ArrowInset = 2.dp
private val TailSize = 18.dp
private val TailOut = 4.dp

/**
 * Przycisk sąsiedniego terenu według makiety hi-fi: nazwa terenu, odległość i strzałka w stronę granicy, przy
 * której stoi widok, a od tej strony ogonek. TalkBack ogłasza go raz po pojawieniu się: „Tu kończy się teren
 * Kampus główny. Obok: Wittigowo, 2 km” (US1.7, NFR-18); cel dotyku ma co najmniej 48 dp (NFR-17).
 *
 * TODO(#788): przycisk nie jest jeszcze podłączony do nakładki mapy.
 *
 * @param from nazwa terenu pokazanego na mapie
 * @param neighbor sąsiedni teren
 * @param onClick dotknięcie przycisku
 * @param modifier położenie na mapie
 */
@Composable
internal fun AreaNeighborButton(
    from: String,
    neighbor: AreaNeighbor,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val distance = stringResource(R.string.mapa_teren_km, neighbor.kilometers)
    val spoken = stringResource(R.string.mapa_teren_sasiad, from, neighbor.name, distance)
    val action = stringResource(R.string.mapa_teren_sasiad_pokaz, neighbor.name)
    Box(modifier) {
        NeighborTail(neighbor.edge)
        val press =
            Modifier
                .clickable(onClickLabel = action, role = Role.Button, onClick = onClick)
                .semantics {
                    contentDescription = spoken
                    liveRegion = LiveRegionMode.Polite
                }
        NeighborPill(neighbor, distance, press)
    }
}

@Composable
private fun NeighborPill(neighbor: AreaNeighbor, distance: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.heightIn(min = MapControlTouchSize),
        shape = CircleShape,
        color = scheme.surfaceContainerLowest,
        shadowElevation = Wysokosci.kontrolkiMapy,
        border = mapControlBorder(),
    ) {
        Row(
            modifier = Modifier.padding(ArrowInset).padding(start = Odstepy.m).clearAndSetSemantics {},
            horizontalArrangement = Arrangement.spacedBy(Odstepy.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = neighbor.name,
                modifier = Modifier.weight(weight = 1f, fill = false),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = Typografia.wyroznienieWaga,
            )
            Text(text = distance, color = scheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            NeighborArrow(neighbor.edge)
        }
    }
}

@Composable
private fun BoxScope.NeighborTail(edge: MapEdge) {
    val (east, south) =
        when (edge) {
            MapEdge.NORTH -> 0f to -1f
            MapEdge.EAST -> 1f to 0f
            MapEdge.SOUTH -> 0f to 1f
            MapEdge.WEST -> -1f to 0f
        }
    val color = MaterialTheme.colorScheme.surfaceContainerLowest
    Box(
        Modifier
            .align(BiasAlignment(horizontalBias = east, verticalBias = south))
            .offset(x = TailOut * east, y = TailOut * south)
            .size(TailSize)
            .rotate(EIGHTH_TURN)
            .background(color, MaterialTheme.shapes.extraSmall),
    )
}

@Composable
private fun NeighborArrow(edge: MapEdge, modifier: Modifier = Modifier) {
    Surface(modifier.size(ControlCircleSize), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
        Box(contentAlignment = Alignment.Center) {
            val turn = Modifier.rotate(edge.ordinal * QUARTER_TURN)
            Icon(imageVector = MapControlIcons.ArrowUp, contentDescription = null, modifier = turn)
        }
    }
}
