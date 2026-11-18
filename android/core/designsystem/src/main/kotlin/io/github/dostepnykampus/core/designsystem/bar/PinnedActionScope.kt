/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem.bar

import androidx.compose.foundation.layout.LayoutScopeMarker
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ParentDataModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import io.github.dostepnykampus.core.designsystem.CeleDotyku

/** Zakres treści [PinnedActionBar]: udział przycisku w szerokości wiersza. */
@LayoutScopeMarker
@Immutable
public interface PinnedActionScope {
    /**
     * Przycisk dzieli z innymi przyciskami z wagą miejsce wiersza, które zostaje po przyciskach bez wagi,
     * proporcjonalnie do dodatniej [weight]. Gdy w tym udziale nie mieści się najdłuższe słowo napisu któregoś
     * przycisku, przycisk przechodzi do następnego wiersza.
     */
    public fun Modifier.weight(weight: Float): Modifier
}

/** Jedyny zakres [PinnedActionScope]: zapisuje wagę w danych rodzica przycisku. */
internal object PinnedActionScopeInstance : PinnedActionScope {
    override fun Modifier.weight(weight: Float): Modifier {
        require(weight > 0f) { "Waga przycisku musi być dodatnia: $weight" }
        return this.then(WeightElement(weight))
    }
}

/** Element modyfikatora wagi [weight] przycisku. */
private data class WeightElement(val weight: Float) : ModifierNodeElement<WeightNode>() {
    override fun create(): WeightNode = WeightNode(weight)

    override fun update(node: WeightNode) {
        node.weight = weight
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "weight"
        value = weight
    }
}

/** Węzeł danych rodzica z wagą [weight] przycisku. */
private class WeightNode(var weight: Float) :
    Modifier.Node(),
    ParentDataModifierNode {
    override fun Density.modifyParentData(parentData: Any?): Any = this@WeightNode
}

/** Przycisk paska: miara, waga (0, gdy szerokość ustala sam przycisk) i zmierzony przycisk bez wagi. */
private class Action(val measurable: Measurable, val placeable: Placeable?) {
    val weight: Float = (measurable.parentData as? WeightNode)?.weight ?: 0f
}

/**
 * Układ przycisków w wierszach z odstępem [CeleDotyku.odstep]. Przyciski bez wagi mierzy w całej szerokości;
 * przycisk zaczyna nowy wiersz, gdy z nim wiersz byłby szerszy od układu albo udział któregoś przycisku z wagą
 * byłby węższy niż jego najmniejsza szerokość własna, czyli najdłuższe słowo napisu.
 */
internal val pinnedActionsPolicy: MeasurePolicy =
    MeasurePolicy { measurables, constraints ->
        val gap = CeleDotyku.odstep.roundToPx()
        val width = constraints.maxWidth
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val lines = mutableListOf<List<Action>>()
        measurables.forEach { measurable ->
            val isWeighted = measurable.parentData is WeightNode
            val action = Action(measurable, if (isWeighted) null else measurable.measure(loose))
            val line = lines.lastOrNull()
            if (line != null && fits(line + action, width, gap)) {
                lines[lines.lastIndex] = line + action
            } else {
                lines += listOf(action)
            }
        }
        val placed = lines.map { line -> measureLine(line, width, gap) }
        val heights = placed.map { line -> line.maxOf { it.height } }
        val height = heights.sum() + gap * (lines.size - 1).coerceAtLeast(0)
        layout(width, height.coerceIn(constraints.minHeight, constraints.maxHeight)) {
            var y = 0
            placed.forEachIndexed { row, line ->
                var x = 0
                line.forEach { placeable ->
                    placeable.placeRelative(x, y)
                    x += placeable.width + gap
                }
                y += heights[row] + gap
            }
        }
    }

/** Szerokość wiersza [line] szerokości [width] pozostała dla przycisków z wagą. */
private fun freeWidth(line: List<Action>, width: Int, gap: Int): Int =
    width - gap * (line.size - 1) - line.sumOf { it.placeable?.width ?: 0 }

/** Udział [action] w szerokości [free] przy sumie wag [total] wiersza. */
private fun share(action: Action, free: Int, total: Float): Int = (free * action.weight / total).toInt()

/** Czy przyciski [line] mieszczą się w jednym wierszu szerokości [width]. */
private fun fits(line: List<Action>, width: Int, gap: Int): Boolean {
    val free = freeWidth(line, width, gap)
    val total = line.sumOf { it.weight.toDouble() }.toFloat()
    return free >= 0 &&
        line.filter { it.placeable == null }.all { action ->
            share(action, free, total) >= action.measurable.minIntrinsicWidth(Constraints.Infinity)
        }
}

/** Mierzy przyciski wiersza [line]; przycisk z wagą dostaje stałą szerokość udziału, ostatni resztę. */
private fun measureLine(line: List<Action>, width: Int, gap: Int): List<Placeable> {
    val free = freeWidth(line, width, gap)
    val total = line.sumOf { it.weight.toDouble() }.toFloat()
    val last = line.lastOrNull { it.placeable == null }
    var left = free
    return line.map { action ->
        action.placeable ?: run {
            val actionWidth = if (action === last) left else share(action, free, total)
            left -= actionWidth
            action.measurable.measure(Constraints.fixedWidth(actionWidth.coerceAtLeast(0)))
        }
    }
}
