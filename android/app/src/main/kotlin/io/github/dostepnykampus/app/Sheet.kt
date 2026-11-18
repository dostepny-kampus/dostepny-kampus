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
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.snapFlingBehavior
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.Wysokosci
import io.github.dostepnykampus.core.designsystem.Zaokraglenia
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Arkusz V2 z wysokościami [SheetHeight], który idzie za palcem i staje według położenia i prędkości. Startuje
 * rozwinięty, gdy [isExpanded], inaczej niski, a gdy [isFullOnly], na całym oknie z samym zamknięciem. Rozwinięcie
 * zgłasza [onExpandedChange], przeciągnięcie w dół do końca [onClose], zasłoniętą część okna w px [onLowHeight].
 * Zmiana [isExpanded] z zewnątrz rozwija niski arkusz do góry okna, a rozwinięty zwija na niską wysokość.
 * Treść [content] dostaje, czy arkusz stoi rozwinięty.
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
    content: @Composable (isFull: Boolean) -> Unit,
) {
    val state = remember { AnchoredDraggableState(startHeight(isExpanded = isExpanded, isFullOnly = isFullOnly)) }
    val measure = remember { SheetMeasure() }
    val jump = remember { SheetJump() }
    val currentOnLowHeight by rememberUpdatedState(onLowHeight)
    FollowHeights(
        state = state,
        measure = measure,
        jump = jump,
        isExpanded = isExpanded,
        isFullOnly = isFullOnly,
        onExpandedChange = onExpandedChange,
        onClose = onClose,
    )
    val isFull = state.currentValue == SheetHeight.Full
    val corner = if (isFull) 0.dp else Zaokraglenia.bardzoDuze
    val density = LocalDensity.current
    val fling =
        remember(state, density) {
            val flick = with(density) { FLICK_VELOCITY.toPx() }
            val snap =
                snapFlingBehavior(
                    snapLayoutInfoProvider = SheetSnap(state = state, flick = flick),
                    decayAnimationSpec = AnchoredDraggableDefaults.DecayAnimationSpec,
                    snapAnimationSpec = AnchoredDraggableDefaults.SnapAnimationSpec,
                )
            SettlingFling(base = snap, state = state)
        }
    val scroll = remember(state, fling) { SheetScrollConnection(state = state, fling = fling) }
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .clipBelowKeepShadow()
                .followHeight(state = state, isCompact = !isExpanded && !isFullOnly) { size ->
                    measure.window = size.window
                    measure.coverOf(restingLow = size.restingLow, isFullOnly = isFullOnly)?.let(currentOnLowHeight)
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
            Handle(state = state, onHeightChoose = { onExpandedChange(it.isExpanded) }, isFullOnly = isFullOnly)
            Box(Modifier.weight(1f, fill = false).fillMaxWidth()) { content(state.settledValue == SheetHeight.Full) }
        }
    }
}

private const val PROVISIONAL_LOW_SHARE = 3

private val FLICK_VELOCITY = 125.dp

/** Zmierzona wysokość okna [window] i niskiej karty [low] w px; zero, dopóki nie zmierzono. */
private class SheetMeasure {
    var window by mutableIntStateOf(0)
    var low by mutableIntStateOf(0)
        private set

    private fun knownLow(): Float = (low.takeIf { it > 0 } ?: (window / PROVISIONAL_LOW_SHARE)).toFloat()

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

    /** Kotwice wysokości [reach] dla zmierzonego okna; przed pomiarem niskiej karty jej wysokość to część okna. */
    fun anchors(isFullOnly: Boolean, reach: Set<SheetHeight>): DraggableAnchors<SheetHeight> {
        if (isFullOnly) return fullOnlyAnchors(window = window.toFloat())
        return sheetAnchors(window = window.toFloat(), low = knownLow(), reach = reach)
    }
}

/** Wysokość [target], do której arkusz jedzie poza gestem, więc jej kotwica jest dostępna; `null` bez takiego ruchu. */
private class SheetJump {
    var target: SheetHeight? by mutableStateOf(null)

    /** Przenosi arkusz [state] na wysokość [height] także dalszą niż o stopień: najpierw dodaje jej kotwicę. */
    suspend fun move(state: AnchoredDraggableState<SheetHeight>, height: SheetHeight) {
        target = height
        try {
            snapshotFlow { state.anchors.hasPositionFor(height) }.first { it }
            state.animateTo(height)
        } finally {
            if (target == height) target = null
        }
    }
}

@Composable
private fun FollowHeights(
    state: AnchoredDraggableState<SheetHeight>,
    measure: SheetMeasure,
    jump: SheetJump,
    isExpanded: Boolean,
    isFullOnly: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onClose: () -> Unit,
) {
    val currentOnExpandedChange by rememberUpdatedState(onExpandedChange)
    val currentOnClose by rememberUpdatedState(onClose)
    val isFullOnlyNow by rememberUpdatedState(isFullOnly)
    val reach = state.settledValue.reach() + setOfNotNull(jump.target)
    val anchors =
        remember(measure.window, measure.low, isFullOnly, reach) {
            measure.anchors(isFullOnly = isFullOnly, reach = reach).takeIf { measure.window > 0 }
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
        if (!isFullOnly && state.settledValue.isExpanded != isExpanded) {
            jump.move(state = state, height = if (isExpanded) SheetHeight.Full else SheetHeight.Low)
        }
    }
}

private fun startHeight(isExpanded: Boolean, isFullOnly: Boolean): SheetHeight =
    when {
        isFullOnly -> SheetHeight.Full
        isExpanded -> SheetHeight.Full
        else -> SheetHeight.Low
    }

private fun DraggableAnchors<SheetHeight>.keeping(height: SheetHeight): SheetHeight =
    if (hasPositionFor(height)) height else SheetHeight.Full

/**
 * Pomiar arkusza: okno [window] i niska karta w spoczynku [restingLow], gdy jest. Mała karta w spoczynku ma
 * wysokość treści, a w ruchu i rozwinięta widoczną część okna.
 */
private data class SheetSize(val window: Int, val restingLow: Int?)

private fun Modifier.followHeight(
    state: AnchoredDraggableState<SheetHeight>,
    isCompact: Boolean,
    onMeasure: (SheetSize) -> Unit,
): Modifier =
    layout { measurable, constraints ->
        val fit = SheetFit(state = state, window = constraints.maxHeight, isCompact = isCompact)
        val placeable = measurable.measure(fit.constraints(constraints))
        onMeasure(SheetSize(window = constraints.maxHeight, restingLow = placeable.height.takeIf { fit.isRestingLow }))
        layout(width = placeable.width, height = placeable.height) { placeable.place(x = 0, y = 0) }
    }

/** Wymiary arkusza za kotwicą [state] w oknie [window]: widoczna część i spoczynek małej karty ([isCompact]). */
private class SheetFit(state: AnchoredDraggableState<SheetHeight>, window: Int, private val isCompact: Boolean) {
    private val offset = state.offset
    private val top = if (offset.isNaN()) 0 else offset.roundToInt()
    private val shown = window - top.coerceIn(minimumValue = 0, maximumValue = window)
    private val anchors = state.anchors
    private val isLow = anchors.hasPositionFor(SheetHeight.Low) && anchors.positionOf(SheetHeight.Low) == offset
    private val isResting = offset.isNaN() || (isCompact && isLow)

    /** Czy to pomiar niskiej karty w spoczynku. */
    val isRestingLow: Boolean get() = isResting && isCompact

    fun constraints(base: Constraints): Constraints =
        if (isResting) base.copy(minHeight = 0) else base.copy(minHeight = shown, maxHeight = shown)
}

private fun Modifier.clipBelowKeepShadow(): Modifier =
    drawWithContent {
        clipRect(left = -size.width, top = -size.height, right = size.width * 2, bottom = size.height) {
            this@drawWithContent.drawContent()
        }
    }

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

/**
 * Cel arkusza [state] po puszczeniu: przy prędkości od [flick] px/s sąsiednia wysokość w stronę rzutu, inaczej
 * najbliższa wysokość.
 *
 * Wymagania: US2.8, NFR-53
 */
private class SheetSnap(private val state: AnchoredDraggableState<SheetHeight>, private val flick: Float) :
    SnapLayoutInfoProvider {
    override fun calculateApproachOffset(velocity: Float, decayOffset: Float): Float = 0f

    override fun calculateSnapOffset(velocity: Float): Float {
        val offset = state.requireOffset()
        return state.anchors.positionOf(target(offset = offset, velocity = velocity)) - offset
    }

    private fun target(offset: Float, velocity: Float): SheetHeight {
        val anchors = state.anchors
        val towards = if (abs(velocity) >= flick) anchors.closestAnchor(offset, velocity > 0f) else null
        return towards ?: anchors.closestAnchor(offset) ?: state.settledValue
    }
}

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
        SheetHeight.Full -> R.string.wysokosc_rozwinieta
        SheetHeight.Low, SheetHeight.Closed -> R.string.wysokosc_niska
    }

private fun Modifier.belowStatusBarWhenFull(statusBar: WindowInsets, isFull: Boolean): Modifier =
    if (isFull) windowInsetsPadding(statusBar) else consumeWindowInsets(statusBar)

@Composable
private fun Handle(
    state: AnchoredDraggableState<SheetHeight>,
    onHeightChoose: (SheetHeight) -> Unit,
    isFullOnly: Boolean,
) {
    val label = stringResource(R.string.wysokosc_karty)
    val stateWord = stringResource(state.currentValue.word())
    val expand = stringResource(R.string.rozwin)
    val collapse = stringResource(R.string.zwin)
    val steps =
        if (isFullOnly) {
            emptyList()
        } else {
            val next = state.currentValue.next()
            listOf(
                CustomAccessibilityAction(if (next == SheetHeight.Full) expand else collapse) {
                    onHeightChoose(next)
                    true
                },
            )
        }
    val tap =
        if (isFullOnly) {
            Modifier
        } else {
            Modifier.clickable(role = Role.Button, onClick = { onHeightChoose(state.currentValue.next()) })
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
