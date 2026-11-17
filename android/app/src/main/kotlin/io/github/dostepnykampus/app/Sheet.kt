/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
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
import androidx.compose.runtime.SideEffect
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
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Arkusz V2 z wysokościami [SheetHeight], który idzie za palcem i staje według położenia i prędkości. Startuje na
 * połowie okna, gdy [isExpanded], inaczej niski, a gdy [isFullOnly], na całym oknie z samym zamknięciem. Rozwinięcie
 * zgłasza [onExpandedChange], przeciągnięcie w dół do końca [onClose], zasłoniętą część okna w px [onLowHeight].
 *
 * Wymagania: US2, US2.8, US2.9, NFR-17, NFR-18, NFR-20, NFR-53
 */
@Composable
internal fun Sheet(
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onClose: () -> Unit,
    onLowHeight: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isFullOnly: Boolean = false,
    content: @Composable () -> Unit,
) {
    val state = remember { AnchoredDraggableState(startHeight(isExpanded = isExpanded, isFullOnly = isFullOnly)) }
    val measure = remember { SheetMeasure() }
    val currentOnLowHeight by rememberUpdatedState(onLowHeight)
    FollowHeights(
        state = state,
        measure = measure,
        isExpanded = isExpanded,
        isFullOnly = isFullOnly,
        onExpandedChange = onExpandedChange,
        onClose = onClose,
    )
    val isFull = state.currentValue == SheetHeight.Full
    val corner = if (isFull) 0.dp else Zaokraglenia.bardzoDuze
    val snap = AnchoredDraggableDefaults.flingBehavior(state)
    val fling = remember(state, snap) { SettlingFling(base = snap, state = state) }
    val scroll = remember(state, fling) { SheetScrollConnection(state = state, fling = fling) }
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .followHeight(state = state, isCompact = !isExpanded && !isFullOnly) { maxHeight, restingLow ->
                    measure.window = maxHeight
                    measure.coverOf(restingLow = restingLow, isFullOnly = isFullOnly)?.let(currentOnLowHeight)
                }.nestedScroll(scroll)
                .anchoredDraggable(
                    state = state,
                    orientation = Orientation.Vertical,
                    flingBehavior = fling,
                ),
        shape = if (isFull) RectangleShape else RoundedCornerShape(topStart = corner, topEnd = corner),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = Wysokosci.pasekCzynnosci,
    ) {
        val statusBar = WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
        Column(modifier = Modifier.belowStatusBarWhenFull(statusBar, isFull)) {
            Handle(state = state, isFullOnly = isFullOnly)
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

    /** Zasłonięta część okna do zgłoszenia: całe okno, gdy [isFullOnly], inaczej nowa niska karta [restingLow]. */
    fun coverOf(restingLow: Int?, isFullOnly: Boolean): Int? =
        when {
            isFullOnly -> window
            keepLow(restingLow) -> low
            else -> null
        }

    /** Kotwice dla zmierzonego okna; przed pomiarem niskiej karty jej wysokość to część okna. */
    fun anchors(isFullOnly: Boolean): DraggableAnchors<SheetHeight> {
        if (isFullOnly) return fullOnlyAnchors(window = window.toFloat())
        val known = low.takeIf { it > 0 } ?: (window / PROVISIONAL_LOW_SHARE)
        return sheetAnchors(window = window.toFloat(), low = known.toFloat())
    }
}

@Composable
private fun FollowHeights(
    state: AnchoredDraggableState<SheetHeight>,
    measure: SheetMeasure,
    isExpanded: Boolean,
    isFullOnly: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onClose: () -> Unit,
) {
    val currentOnExpandedChange by rememberUpdatedState(onExpandedChange)
    val currentOnClose by rememberUpdatedState(onClose)
    val isFullOnlyNow by rememberUpdatedState(isFullOnly)
    val anchors =
        remember(key1 = measure.window, key2 = measure.low, key3 = isFullOnly) {
            measure.anchors(isFullOnly = isFullOnly).takeIf { measure.window > 0 }
        }
    SideEffect { anchors?.let { state.updateAnchors(newAnchors = it, newTarget = it.keeping(state.targetValue)) } }
    LaunchedEffect(state) {
        snapshotFlow { state.settledValue }.collect { height ->
            when {
                height == SheetHeight.Closed -> currentOnClose()
                !isFullOnlyNow -> currentOnExpandedChange(height.isExpanded)
            }
        }
    }
    LaunchedEffect(isExpanded) {
        if (!isExpanded && !isFullOnly && state.settledValue.isExpanded) state.animateTo(SheetHeight.Low)
    }
}

private fun startHeight(isExpanded: Boolean, isFullOnly: Boolean): SheetHeight =
    when {
        isFullOnly -> SheetHeight.Full
        isExpanded -> SheetHeight.Half
        else -> SheetHeight.Low
    }

private fun DraggableAnchors<SheetHeight>.keeping(height: SheetHeight): SheetHeight =
    if (hasPositionFor(height)) height else SheetHeight.Full

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
        val top = offset.roundToIntOrZero().coerceIn(minimumValue = 0, maximumValue = constraints.maxHeight)
        val shown = constraints.maxHeight - top
        val fixed =
            if (isResting) constraints.copy(minHeight = 0) else constraints.copy(minHeight = shown, maxHeight = shown)
        val placeable = measurable.measure(fixed)
        onMeasure(constraints.maxHeight, placeable.height.takeIf { isResting && isCompact })
        layout(width = placeable.width, height = placeable.height) { placeable.place(x = 0, y = 0) }
    }

private fun Float.roundToIntOrZero(): Int = if (isNaN()) 0 else roundToInt()

/**
 * Przewijanie treści przesuwa najpierw arkusz w górę, a resztę przewinięcia w dół oddaje arkuszowi. Arkusz
 * przesunięty gestem na treści staje po puszczeniu według prędkości rzutu zachowaniem [fling], tak jak po geście
 * na uchwycie.
 */
private class SheetScrollConnection(
    private val state: AnchoredDraggableState<SheetHeight>,
    private val fling: FlingBehavior,
) : NestedScrollConnection {
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
        settle(velocity = available.y)
        return available
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        if (isOnAnchor()) return Velocity.Zero
        settle(velocity = available.y)
        return available
    }

    private fun isOnAnchor(): Boolean {
        val nearest = state.anchors.closestAnchor(state.offset) ?: return true
        return abs(state.anchors.positionOf(nearest) - state.offset) <= ANCHOR_TOLERANCE_PX
    }

    private suspend fun settle(velocity: Float) {
        state.anchoredDrag { anchors ->
            val follow =
                object : ScrollScope {
                    override fun scrollBy(pixels: Float): Float {
                        val before = state.offset
                        val after =
                            (before + pixels).coerceIn(
                                minimumValue = anchors.minPosition(),
                                maximumValue = anchors.maxPosition(),
                            )
                        dragTo(newOffset = after)
                        return after - before
                    }
                }
            with(fling) { follow.performFling(initialVelocity = velocity) }
        }
    }
}

private const val ANCHOR_TOLERANCE_PX = 0.5f

/** Rzut [base], po którym arkusz [state] stoi dokładnie na najbliższej kotwicy, więc jego wysokość się ustala. */
private class SettlingFling(private val base: FlingBehavior, private val state: AnchoredDraggableState<SheetHeight>) :
    FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        val left = with(base) { performFling(initialVelocity = initialVelocity) }
        val nearest = state.anchors.closestAnchor(state.offset)
        if (nearest != null) scrollBy(pixels = state.anchors.positionOf(nearest) - state.offset)
        return left
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
private fun Handle(state: AnchoredDraggableState<SheetHeight>, isFullOnly: Boolean) {
    val scope = rememberCoroutineScope()
    val label = stringResource(R.string.wysokosc_karty)
    val stateWord = stringResource(state.currentValue.word())
    val expand = stringResource(R.string.rozwin)
    val collapse = stringResource(R.string.zwin)
    val goTo = { target: SheetHeight -> scope.launch { state.animateTo(target) } }
    val steps =
        if (isFullOnly) {
            emptyList()
        } else {
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
        }
    val tap =
        if (isFullOnly) {
            Modifier
        } else {
            Modifier.clickable(role = Role.Button, onClick = { goTo(state.currentValue.next()) })
        }
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = CeleDotyku.minimum)
                .then(tap)
                .semantics {
                    contentDescription = label
                    stateDescription = stateWord
                    customActions = steps
                },
        contentAlignment = Alignment.Center,
    ) {
        val grip = Modifier.size(width = 32.dp, height = 4.dp)
        Box(modifier = grip.background(color = MaterialTheme.colorScheme.outline, shape = CircleShape))
    }
}
