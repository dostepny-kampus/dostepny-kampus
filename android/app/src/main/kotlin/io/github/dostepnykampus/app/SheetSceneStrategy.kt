/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
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
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.Wysokosci
import io.github.dostepnykampus.core.designsystem.Zaokraglenia
import io.github.dostepnykampus.feature.map.CardMode
import io.github.dostepnykampus.feature.map.navigation.BuildingRoute
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

private const val SHEET_KEY = "arkusz"
private const val MAP_KEY = "mapa"
private const val MATERIAL_DURATION_MEDIUM2_MILLIS = 300
private val MaterialStandardEasing = CubicBezierEasing(a = 0.2f, b = 0f, c = 0f, d = 1f)
private val MaterialStandardMediumSlide =
    tween<Float>(durationMillis = MATERIAL_DURATION_MEDIUM2_MILLIS, easing = MaterialStandardEasing)

private val SheetStateSaver =
    Saver<SheetState, Boolean>(
        save = { it.isExpanded },
        restore = { SheetState(isExpanded = it) },
    )

/**
 * Część V1 zasłonięta przez arkusz [cover] i rozwinięcie arkusza [isExpanded] dla trybu karty w nim.
 *
 * Wymagania: US2.8
 */
internal data class SheetLayout(val cover: Dp = 0.dp, val isExpanded: Boolean = false) :
    PaddingValues by PaddingValues(bottom = cover)

/**
 * Część V1 zasłonięta przez arkusz V2, jako dopełnienie od dołu; bez arkusza zero. Mapa rysuje się dalej pod
 * arkuszem, a ekran V1 przesuwa o nie tylko swoje przyciski i podpis OSM, żeby stały nad arkuszem.
 * Treść arkusza dostaje tu [SheetLayout] bez dopełnienia, z rozwinięciem arkusza, z którego karta bierze tryb.
 *
 * Wymagania: NFR-37, US2.8
 */
internal val LocalSheetPadding = compositionLocalOf { SheetLayout() }

/**
 * Strategie sceny w oknie: bez arkusza, gdy karta V2 jest od razu na pełnym ekranie.
 *
 * Wymagania: US2.9, NFR-20, NFR-51
 */
internal fun CardWindow.sceneStrategies(): List<SceneStrategy<NavKey>> =
    if (cardMode() == CardMode.Direct) emptyList() else listOf(SheetSceneStrategy)

/**
 * Strategia sceny dla V2: wpis z metadanymi arkusza ([metadata]) nad samym V1 rysuje się jako arkusz od dołu nad żywym
 * V1, chyba że V2 otwiera się od razu na pełnym ekranie ([sceneStrategies]); otwarcie i zamknięcie V2 nie zmienia
 * sceny. Przewidywany gest „Wstecz” przesuwa arkusz za palcem, po anulowaniu arkusz wraca, a po zakończeniu pełny ekran
 * zwija się do małej karty, która dojeżdża w dół i raz zamyka V2; „Wstecz” w trakcie dojazdu jest pochłaniane. Ruch ma
 * czas `medium2` i krzywą standardową Material 3. Scena, z której NavDisplay przechodzi do innej, nie odbiera „Wstecz”.
 * Inne układy historii zostawia scenie z jednym ekranem.
 *
 * Wymagania: NFR-15, US3
 */
internal object SheetSceneStrategy : SceneStrategy<NavKey> {
    private val sheet: Map<String, Any> = mapOf(SHEET_KEY to true)
    private val map: Map<String, Any> = mapOf(MAP_KEY to true)

    /** Metadane wpisu [route]: V1 to mapa, V2 arkusz nad V1, chyba że [BuildingRoute.isDirect], inne trasy bez. */
    fun metadata(route: NavKey): Map<String, Any> =
        when (route) {
            is MapRoute -> map
            is BuildingRoute -> if (route.isDirect) emptyMap() else sheet
            else -> emptyMap()
        }

    override fun SceneStrategyScope<NavKey>.calculateScene(entries: List<NavEntry<NavKey>>): Scene<NavKey>? {
        val isMapAlone = entries.size == 1 && MAP_KEY in entries.first().metadata
        val isSheetOverMap = entries.size == 2 && SHEET_KEY in entries.last().metadata
        return when {
            isMapAlone -> SheetScene(map = entries.first(), sheet = null, onBack = onBack)
            isSheetOverMap -> SheetScene(map = entries.first(), sheet = entries.last(), onBack = onBack)
            else -> null
        }
    }
}

private data class SheetScene(val map: NavEntry<NavKey>, val sheet: NavEntry<NavKey>?, val onBack: () -> Unit) :
    Scene<NavKey> {
    override val key: Any = map.contentKey
    override val entries: List<NavEntry<NavKey>> = listOfNotNull(map, sheet)
    override val previousEntries: List<NavEntry<NavKey>> = if (sheet == null) emptyList() else listOf(map)
    override val content: @Composable () -> Unit = {
        var state by rememberSaveable(sheet?.contentKey, stateSaver = SheetStateSaver) { mutableStateOf(SheetState()) }
        val shift = remember(sheet?.contentKey) { Animatable(0f) }
        var isClosing by remember(sheet?.contentKey) { mutableStateOf(false) }
        var height by remember(sheet?.contentKey) { mutableStateOf(0.dp) }
        val density = LocalDensity.current
        val scope = rememberCoroutineScope()
        Box(modifier = Modifier.fillMaxSize()) {
            val cover = if (sheet == null) 0.dp else height
            val silentUnderSheet = Modifier.semantics { if (sheet != null) hideFromAccessibility() }
            CompositionLocalProvider(LocalSheetPadding provides SheetLayout(cover = cover)) {
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
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .onSizeChanged { height = with(density) { it.height.toDp() } }
                            .offset { IntOffset(x = 0, y = sheetBackOffset(shift.value, height).roundToPx()) },
                ) {
                    val layout = SheetLayout(isExpanded = state.isExpanded)
                    CompositionLocalProvider(LocalSheetPadding provides layout) { sheet.Content() }
                }
            }
        }
    }
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

/**
 * Arkusz V2 o wysokości treści albo rozwinięty na całe okno ([isExpanded]); zmianę rozwinięcia zgłasza
 * [onExpandedChange]. Dotknięcie uchwytu, przewinięcie treści albo przeciągnięcie w górę rozwija go, więc przeciąganie
 * nie jest jedyną drogą (wymagania V2). Tło `surfaceContainer`, górne rogi `Zaokraglenia.bardzoDuze`, cień
 * `Wysokosci.pasekCzynnosci` padający w górę, jak w makiecie hi-fi V2.
 */
@Composable
internal fun Sheet(
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val isCurrentlyExpanded by rememberUpdatedState(isExpanded)
    val currentOnExpandedChange by rememberUpdatedState(onExpandedChange)
    val expandOnScroll =
        remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (isCurrentlyExpanded || available.y >= 0f) return Offset.Zero
                    currentOnExpandedChange(true)
                    return available
                }
            }
        }
    val corner = if (isExpanded) 0.dp else Zaokraglenia.bardzoDuze
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .then(if (isExpanded) Modifier.fillMaxHeight() else Modifier)
                .nestedScroll(expandOnScroll),
        shape = if (isExpanded) RectangleShape else RoundedCornerShape(topStart = corner, topEnd = corner),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = Wysokosci.pasekCzynnosci,
    ) {
        val statusBar = WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
        Column(modifier = Modifier.belowStatusBarWhenExpanded(statusBar, isExpanded)) {
            Handle(isExpanded = isExpanded, onClick = { onExpandedChange(!isExpanded) })
            Box(Modifier.weight(1f, fill = false).fillMaxWidth()) { content() }
        }
    }
}

private fun Modifier.belowStatusBarWhenExpanded(statusBar: WindowInsets, isExpanded: Boolean): Modifier =
    if (isExpanded) windowInsetsPadding(statusBar) else consumeWindowInsets(statusBar)

@Composable
private fun Handle(isExpanded: Boolean, onClick: () -> Unit) {
    val label = stringResource(R.string.rozwin_opis)
    val state = stringResource(if (isExpanded) R.string.stan_rozwiniete else R.string.stan_zwiniete)
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = CeleDotyku.minimum)
                .clickable(role = Role.Button, onClick = onClick)
                .scrollable(rememberScrollableState { 0f }, Orientation.Vertical)
                .semantics {
                    contentDescription = label
                    stateDescription = state
                },
        contentAlignment = Alignment.Center,
    ) {
        val grip = Modifier.size(width = 32.dp, height = 4.dp)
        Box(modifier = grip.background(color = MaterialTheme.colorScheme.outline, shape = CircleShape))
    }
}
