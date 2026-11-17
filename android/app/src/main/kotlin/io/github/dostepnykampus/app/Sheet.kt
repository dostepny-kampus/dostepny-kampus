/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.annotation.StringRes
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.Wysokosci
import io.github.dostepnykampus.core.designsystem.Zaokraglenia
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Arkusz V2 z wysokościami [SheetHeight]: idzie za palcem po uchwycie i po treści, a po puszczeniu staje na
 * najbliższej wysokości. Startuje rozwinięty na połowę okna, gdy [isExpanded], inaczej niski; zmianę rozwinięcia
 * zgłasza [onExpandedChange], a [isExpanded] równe `false` zwija go do niskiej. Przeciągnięcie w dół z niskiej
 * wywołuje [onClose]. Wysokość niskiej karty w px, liczoną z treści, dostaje [onLowHeight]. Dotknięcie uchwytu
 * przechodzi na następną wysokość, a czytnik ekranu ma czynności rozwinięcia i zwinięcia, więc przeciąganie nie jest
 * jedyną drogą (WCAG 2.5.7).
 *
 * Wymagania: US2, US2.8, NFR-17, NFR-18, NFR-53
 */
@Composable
internal fun Sheet(
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onClose: () -> Unit,
    onLowHeight: (Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val state = remember { AnchoredDraggableState(if (isExpanded) SheetHeight.Half else SheetHeight.Low) }
    val measure = remember { SheetMeasure() }
    val currentOnLowHeight by rememberUpdatedState(onLowHeight)
    FollowHeights(state, measure, isExpanded, onExpandedChange, onClose)
    val isFull = state.currentValue == SheetHeight.Full
    val corner = if (isFull) 0.dp else Zaokraglenia.bardzoDuze
    val scroll = remember(state) { SheetScrollConnection(state) }
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .followHeight(state = state, isCompact = !isExpanded) { maxHeight, restingLow ->
                    measure.window = maxHeight
                    if (measure.keepLow(restingLow)) currentOnLowHeight(measure.low)
                }.nestedScroll(scroll)
                .anchoredDraggable(
                    state = state,
                    orientation = Orientation.Vertical,
                    flingBehavior = AnchoredDraggableDefaults.flingBehavior(state),
                ),
        shape = if (isFull) RectangleShape else RoundedCornerShape(topStart = corner, topEnd = corner),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = Wysokosci.pasekCzynnosci,
    ) {
        val statusBar = WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
        Column(modifier = Modifier.belowStatusBarWhenFull(statusBar, isFull)) {
            Handle(state = state)
            Box(Modifier.weight(1f, fill = false).fillMaxWidth()) { content() }
        }
    }
}

private const val PROVISIONAL_LOW_SHARE = 3

/** Zmierzona wysokość okna [window] i niskiej karty [low] w px; zero, dopóki nie zmierzono. */
private class SheetMeasure {
    var window by mutableIntStateOf(0)
    var low by mutableIntStateOf(0)
        private set

    /** Zapamiętuje wysokość niskiej karty [measured], gdy jest; `true`, gdy się zmieniła. */
    fun keepLow(measured: Int?): Boolean {
        if (measured == null || measured == low) return false
        low = measured
        return true
    }

    /** Kotwice dla zmierzonego okna; przed pomiarem niskiej karty jej wysokość to część okna. */
    fun anchors(): DraggableAnchors<SheetHeight> {
        val known = low.takeIf { it > 0 } ?: (window / PROVISIONAL_LOW_SHARE)
        return sheetAnchors(window.toFloat(), known.toFloat())
    }
}

/**
 * Kotwice [state] z pomiarów [measure], zgłoszenie rozwinięcia przez [onExpandedChange] albo zamknięcia przez
 * [onClose] po staniu na wysokości i zwinięcie do niskiej, gdy [isExpanded] spada na `false`.
 */
@Composable
private fun FollowHeights(
    state: AnchoredDraggableState<SheetHeight>,
    measure: SheetMeasure,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onClose: () -> Unit,
) {
    val currentOnExpandedChange by rememberUpdatedState(onExpandedChange)
    val currentOnClose by rememberUpdatedState(onClose)
    LaunchedEffect(measure.window, measure.low) {
        if (measure.window > 0) state.updateAnchors(measure.anchors())
    }
    LaunchedEffect(state) {
        snapshotFlow { state.settledValue }.collect { height ->
            if (height == SheetHeight.Closed) currentOnClose() else currentOnExpandedChange(height.isExpanded)
        }
    }
    LaunchedEffect(isExpanded) {
        if (!isExpanded && state.settledValue.isExpanded) state.animateTo(SheetHeight.Low)
    }
}

/**
 * Wysokość arkusza ze stanu [state]: na niskiej wysokości z małą kartą ([isCompact]) i przed pierwszym pomiarem
 * wysokość treści, inaczej część okna pod górą arkusza. [onMeasure] dostaje wysokość okna i wysokość niskiej karty
 * w px albo `null`, gdy tej nie da się zmierzyć.
 */
private fun Modifier.followHeight(
    state: AnchoredDraggableState<SheetHeight>,
    isCompact: Boolean,
    onMeasure: (Int, Int?) -> Unit,
): Modifier =
    layout { measurable, constraints ->
        val offset = state.offset
        val isLow =
            state.anchors.hasPositionFor(SheetHeight.Low) && state.anchors.positionOf(SheetHeight.Low) == offset
        val isResting = offset.isNaN() || (isCompact && isLow)
        val shown = (constraints.maxHeight - offset.roundToIntOrZero()).coerceIn(0, constraints.maxHeight)
        val fixed =
            if (isResting) constraints.copy(minHeight = 0) else constraints.copy(minHeight = shown, maxHeight = shown)
        val placeable = measurable.measure(fixed)
        onMeasure(constraints.maxHeight, placeable.height.takeIf { isResting && isCompact })
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }

private fun Float.roundToIntOrZero(): Int = if (isNaN()) 0 else roundToInt()

/** Przewijanie treści przesuwa najpierw arkusz w górę, a resztę przewinięcia w dół oddaje arkuszowi. */
private class SheetScrollConnection(private val state: AnchoredDraggableState<SheetHeight>) : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
        if (available.y < 0f && source == NestedScrollSource.UserInput) {
            Offset(x = 0f, y = state.dispatchRawDelta(available.y))
        } else {
            Offset.Zero
        }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
        if (source == NestedScrollSource.UserInput) {
            Offset(x = 0f, y = state.dispatchRawDelta(available.y))
        } else {
            Offset.Zero
        }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (available.y >= 0f || state.offset <= state.anchors.minPosition()) return Velocity.Zero
        state.settle(spring())
        return available
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        state.settle(spring())
        return available
    }
}

@StringRes
private fun SheetHeight.word(): Int =
    when (this) {
        SheetHeight.Full -> R.string.wysokosc_caly
        SheetHeight.Half -> R.string.wysokosc_polowa
        SheetHeight.Low, SheetHeight.Closed -> R.string.wysokosc_niska
    }

private fun Modifier.belowStatusBarWhenFull(statusBar: WindowInsets, isFull: Boolean): Modifier =
    if (isFull) windowInsetsPadding(statusBar) else consumeWindowInsets(statusBar)

@Composable
private fun Handle(state: AnchoredDraggableState<SheetHeight>) {
    val scope = rememberCoroutineScope()
    val label = stringResource(R.string.wysokosc_karty)
    val stateWord = stringResource(state.currentValue.word())
    val expand = stringResource(R.string.rozwin)
    val collapse = stringResource(R.string.zwin)
    val goTo = { target: SheetHeight -> scope.launch { state.animateTo(target) } }
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = CeleDotyku.minimum)
                .clickable(role = Role.Button, onClick = { goTo(state.currentValue.next()) })
                .semantics {
                    contentDescription = label
                    stateDescription = stateWord
                    customActions =
                        listOfNotNull(
                            state.currentValue.higher()?.let { up ->
                                CustomAccessibilityAction(expand) {
                                    goTo(up)
                                    true
                                }
                            },
                            state.currentValue.lower()?.let { down ->
                                CustomAccessibilityAction(collapse) {
                                    goTo(down)
                                    true
                                }
                            },
                        )
                },
        contentAlignment = Alignment.Center,
    ) {
        val grip = Modifier.size(width = 32.dp, height = 4.dp)
        Box(modifier = grip.background(color = MaterialTheme.colorScheme.outline, shape = CircleShape))
    }
}
