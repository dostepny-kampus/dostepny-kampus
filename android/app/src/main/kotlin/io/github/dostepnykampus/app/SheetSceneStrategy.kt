/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import io.github.dostepnykampus.app.navigation.CardWindow
import io.github.dostepnykampus.app.navigation.isWideOrLandscape
import io.github.dostepnykampus.feature.map.camera.MOVE_WAIT_MILLIS
import io.github.dostepnykampus.feature.map.camera.MapMotion
import io.github.dostepnykampus.feature.map.navigation.BuildingRoute
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val SHEET_KEY = "arkusz"
private const val MAP_KEY = "mapa"
private const val MATERIAL_DURATION_MEDIUM2_MILLIS = 300

/** Znacznik testowy obrazu zamkniętej karty, która zjeżdża w dół. */
internal const val CLOSING_SHEET_TAG = "zamykana_karta"

private val MaterialStandardEasing = CubicBezierEasing(a = 0.2f, b = 0f, c = 0f, d = 1f)
private val MaterialStandardMediumSlide =
    tween<Float>(durationMillis = MATERIAL_DURATION_MEDIUM2_MILLIS, easing = MaterialStandardEasing)

private val SheetStateSaver =
    Saver<SheetState, Boolean>(
        save = { it.isExpanded },
        restore = { SheetState(isExpanded = it) },
    )

/**
 * Część V1 zasłonięta przez arkusz [cover], rozwinięcie arkusza [isExpanded] dla trybu karty w nim i postój arkusza
 * na całym oknie [isFull]. [isPending]: nowy arkusz nie ma jeszcze pomiaru, a [cover] to zasłonięcie sprzed niego.
 * [onExpand] podnosi niski arkusz na połowę ekranu.
 *
 * Wymagania: US2.8, NFR-56
 */
internal data class SheetLayout(
    val cover: Dp = 0.dp,
    val isExpanded: Boolean = false,
    val isFull: Boolean = false,
    val isPending: Boolean = false,
    val onExpand: () -> Unit = {},
) : PaddingValues by PaddingValues(bottom = cover)

/**
 * Część V1 zasłonięta przez arkusz V2, jako dopełnienie od dołu; bez arkusza zero. Mapa rysuje się dalej pod
 * arkuszem, a ekran V1 przesuwa o nie tylko swoje przyciski i podpis OSM, żeby stały nad arkuszem.
 * Treść arkusza dostaje tu [SheetLayout] bez dopełnienia, z rozwinięciem arkusza, z którego karta bierze tryb.
 *
 * Wymagania: NFR-37, US2.8
 */
internal val LocalSheetPadding = compositionLocalOf { SheetLayout() }

/**
 * Strategie sceny w oknie: bez arkusza w oknie szerokim albo poziomym, gdzie karta V2 zastępuje mapę.
 *
 * Wymagania: US2.9, NFR-20, NFR-51
 */
internal fun CardWindow.sceneStrategies(sheet: SheetSceneStrategy): List<SceneStrategy<NavKey>> =
    if (isWideOrLandscape) emptyList() else listOf(sheet)

/**
 * Strategia sceny dla V2: wpis z metadanymi arkusza ([metadata]) nad samym V1 rysuje się jako arkusz od dołu nad żywym
 * V1, chyba że V2 otwiera się od razu na pełnym ekranie ([sceneStrategies]). Przewidywany gest „Wstecz” przesuwa arkusz
 * za palcem, a po zakończeniu pełny ekran zwija się do małej karty, która dojeżdża w dół i raz zamyka V2. Zamknięty
 * arkusz wjeżdża w czasie i po krzywej ruchu mapy z [motion], otwarty tylko zmienia treść. Scena, z której NavDisplay
 * przechodzi do innej, nie odbiera „Wstecz”. Inne układy historii zostawia scenie z jednym ekranem.
 *
 * Wymagania: NFR-15, US3, NFR-56
 */
internal class SheetSceneStrategy(private val motion: MapMotion) : SceneStrategy<NavKey> {
    override fun SceneStrategyScope<NavKey>.calculateScene(entries: List<NavEntry<NavKey>>): Scene<NavKey>? {
        val base = entries.firstOrNull() ?: return null
        val isMapAlone = entries.size == 1 && MAP_KEY in base.metadata
        val isSheetOverMap = entries.size == 2 && SHEET_KEY in entries.last().metadata
        return when {
            isMapAlone -> SheetScene(map = base, sheet = null, motion = motion, onBack = onBack)
            isSheetOverMap -> SheetScene(map = base, sheet = entries.last(), motion = motion, onBack = onBack)
            else -> null
        }
    }

    /** Metadane wpisów sceny. */
    companion object {
        private val sheet: Map<String, Any> = mapOf(SHEET_KEY to true)
        private val map: Map<String, Any> = mapOf(MAP_KEY to true)

        /** Metadane wpisu [route]: V1 to mapa, V2 arkusz nad V1, chyba że [BuildingRoute.isDirect], inne trasy bez. */
        fun metadata(route: NavKey): Map<String, Any> =
            when {
                route is MapRoute -> map
                isSheet(route) -> sheet
                else -> emptyMap()
            }

        /** [route] to V2 rysowane jako arkusz nad V1. */
        fun isSheet(route: NavKey): Boolean = route is BuildingRoute && !route.isDirect
    }
}

private data class SheetScene(
    val map: NavEntry<NavKey>,
    val sheet: NavEntry<NavKey>?,
    val motion: MapMotion,
    val onBack: () -> Unit,
) : Scene<NavKey> {
    override val key: Any = map.contentKey
    override val entries: List<NavEntry<NavKey>> = listOfNotNull(map, sheet)
    override val previousEntries: List<NavEntry<NavKey>> = if (sheet == null) emptyList() else listOf(map)
    override val content: @Composable () -> Unit = {
        var state by rememberSaveable(sheet != null, stateSaver = SheetStateSaver) { mutableStateOf(SheetState()) }
        val shift = remember(sheet?.contentKey) { Animatable(0f) }
        var isClosing by remember(sheet?.contentKey) { mutableStateOf(false) }
        var height by remember(sheet != null) { mutableStateOf(0.dp) }
        val hidden = rememberSheetEntrance(sheet = sheet?.contentKey, motion = motion)
        val density = LocalDensity.current
        val scope = rememberCoroutineScope()
        val picture = rememberGraphicsLayer()
        val expand = remember(sheet != null) { { state = state.copy(isExpanded = true) } }
        val exit = rememberSheetExit(isShown = sheet != null)
        val isPending = sheet != null && height == 0.dp
        val cover = rememberHeldWhile(isHeld = isPending, value = if (sheet == null) 0.dp else height)
        Box(modifier = Modifier.fillMaxSize()) {
            val silentUnderSheet = Modifier.semantics { if (sheet != null) hideFromAccessibility() }
            CompositionLocalProvider(LocalSheetPadding provides SheetLayout(cover = cover, isPending = isPending)) {
                Box(modifier = silentUnderSheet) { map.Content() }
            }
            if (sheet != null) {
                val isTarget = LocalNavAnimatedContentScope.current.transition.targetState == EnterExitState.Visible
                SheetBackHandler(isEnabled = isTarget) { gesture ->
                    if (!isClosing) {
                        isClosing = state.closesOn(gesture)
                        state = state.after(gesture)
                        scope.launch(start = CoroutineStart.UNDISPATCHED) {
                            if (gesture.isSlide) {
                                shift.animateTo(state.progress, MaterialStandardMediumSlide)
                            } else {
                                shift.snapTo(state.progress)
                            }
                            if (isClosing) onBack()
                        }
                    }
                }
                Sheet(
                    isExpanded = state.isExpanded,
                    onExpandedChange = { state = state.copy(isExpanded = it) },
                    onClose = onBack,
                    onLowHeight = { height = with(density) { it.toDp() } },
                    isFullOnly = isFullOnlySheet(density.fontScale),
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(constraints)
                                layout(width = placeable.width, height = placeable.height) {
                                    val down = sheetBackOffset(shift.value + hidden.value, placeable.height.toDp())
                                    exit.keep(height = placeable.height, offset = shift.value + hidden.value)
                                    placeable.place(x = 0, y = down.roundToPx())
                                }
                            }.drawWithContent {
                                val sheetContent = this
                                picture.record { sheetContent.drawContent() }
                                drawLayer(picture)
                            },
                ) { isFull ->
                    val layout = SheetLayout(isExpanded = state.isExpanded, isFull = isFull, onExpand = expand)
                    CompositionLocalProvider(LocalSheetPadding provides layout) { sheet.Content() }
                }
            } else if (exit.isLeaving) {
                ClosingSheet(exit = exit, picture = picture, modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

/**
 * Zaznaczenie [focus] z trasy V1 do pokazania na mapie. Gdy nad V1 jest arkusz ([isSheetAbove]) bez pomiaru, mapa
 * dostaje poprzednie zaznaczenie, więc zaznaczenie i wysokość karty docierają do niej w tej samej klatce.
 *
 * Wymagania: NFR-56, US2.8
 */
@Composable
internal fun shownFocus(focus: MapFocus?, isSheetAbove: Boolean): MapFocus? {
    val sheet = LocalSheetPadding.current
    return rememberHeldWhile(isHeld = isSheetAbove && (sheet.isPending || sheet.cover == 0.dp), value = focus)
}

/** [value] albo, gdy [isHeld], wartość pokazana w poprzedniej kompozycji. */
@Composable
internal fun <T> rememberHeldWhile(isHeld: Boolean, value: T): T {
    val last = remember { HeldValue(value) }
    val shown = last.shown(isHeld = isHeld, current = value)
    SideEffect { last.keep(shown) }
    return shown
}

private class HeldValue<T>(private var last: T) {
    fun shown(isHeld: Boolean, current: T): T = if (isHeld) last else current

    fun keep(shown: T) {
        last = shown
    }
}

/**
 * Zjazd zamkniętej karty: ostatni obraz arkusza o wysokości [height] w px, schowany pod dolną krawędzią w części
 * [gone]. Karta zamknięta przez „Zamknij”, dotknięcie mapy albo „Wstecz” zjeżdża z miejsca, w którym stała, w czasie
 * `medium2`, jak przelot mapy do widoku sprzed karty; karta już schowana gestem „Wstecz” nie zjeżdża drugi raz.
 * Skala animacji 0 kończy zjazd od razu. Wymagania: NFR-56, US2.8
 */
internal class SheetExit {
    var height: Int = 0
        private set
    private var offset = 0f
    val gone = Animatable(1f)
    val isLeaving: Boolean by derivedStateOf { gone.value < 1f }

    fun keep(height: Int, offset: Float) {
        this.height = height
        this.offset = offset
    }

    suspend fun leave() {
        if (height == 0 || offset >= 1f) return
        gone.snapTo(offset)
        gone.animateTo(targetValue = 1f, animationSpec = MaterialStandardMediumSlide)
    }

    suspend fun show() = gone.snapTo(1f)
}

@Composable
internal fun rememberSheetExit(isShown: Boolean): SheetExit {
    val exit = remember { SheetExit() }
    LaunchedEffect(isShown) { if (isShown) exit.show() else exit.leave() }
    return exit
}

@Composable
internal fun ClosingSheet(exit: SheetExit, picture: GraphicsLayer, modifier: Modifier = Modifier) {
    val height = with(LocalDensity.current) { exit.height.toDp() }
    Box(
        modifier =
            modifier
                .offset { IntOffset(x = 0, y = (exit.gone.value * exit.height).roundToInt()) }
                .fillMaxWidth()
                .height(height)
                .testTag(CLOSING_SHEET_TAG)
                .semantics { hideFromAccessibility() }
                .drawWithContent { drawLayer(picture) },
    )
}

/**
 * Część wysokości arkusza [sheet] schowana pod dolną krawędzią: zamknięty arkusz zaczyna schowany i wjeżdża w czasie
 * następnego ruchu mapy z [motion], po krzywej [MapMotion.Easing]; czas ma już skalę animacji systemu. Bez ruchu mapy
 * w ciągu [MOVE_WAIT_MILLIS] wjeżdża w czasie `medium2`. Arkusz otwarty, który tylko zmienia treść, nie wjeżdża.
 *
 * Wymagania: NFR-56, US2.8
 */
@Composable
internal fun rememberSheetEntrance(sheet: Any?, motion: MapMotion): Animatable<Float, AnimationVector1D> {
    val wasShown = remember { mutableStateOf(false) }
    val hidden = remember(sheet) { Animatable(if (sheet != null && !wasShown.value) 1f else 0f) }
    val after = remember(sheet) { motion.moves }
    SideEffect { wasShown.value = sheet != null }
    LaunchedEffect(hidden) {
        if (hidden.value != 0f) {
            motion.slide(value = hidden, target = 0f, after = after, fallback = MaterialStandardMediumSlide)
        }
    }
    return hidden
}

/**
 * Przewidywany gest „Wstecz” nad arkuszem V2: gdy [isEnabled], każde zdarzenie gestu (początek, postęp, anulowanie,
 * zakończenie) trafia do [onGesture], zanim zobaczy je NavDisplay, więc scena V1 pod arkuszem zostaje bez zmian.
 */
@Composable
internal fun SheetBackHandler(isEnabled: Boolean, onGesture: (BackGesture) -> Unit) {
    val dispatcher = checkNotNull(LocalNavigationEventDispatcherOwner.current).navigationEventDispatcher
    val currentOnGesture by rememberUpdatedState(onGesture)
    val handler =
        remember(dispatcher) {
            object : NavigationEventHandler<NavigationEventInfo.None>(NavigationEventInfo.None, isEnabled) {
                override fun onBackStarted(event: NavigationEvent) = currentOnGesture(BackGesture.Started)

                override fun onBackProgressed(event: NavigationEvent) =
                    currentOnGesture(BackGesture.Progressed(event.progress))

                override fun onBackCancelled() = currentOnGesture(BackGesture.Cancelled)

                override fun onBackCompleted() = currentOnGesture(BackGesture.Completed)
            }
        }
    SideEffect { handler.isBackEnabled = isEnabled }
    DisposableEffect(handler) {
        dispatcher.addHandler(handler)
        onDispose { handler.remove() }
    }
}
