/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.Wysokosci
import io.github.dostepnykampus.core.designsystem.Zaokraglenia
import io.github.dostepnykampus.feature.map.navigation.BuildingRoute
import io.github.dostepnykampus.feature.map.navigation.MapRoute

private const val SHEET_KEY = "arkusz"
private const val MAP_KEY = "mapa"
private const val HALF_WINDOW = 0.5f

private val SheetStateSaver =
    Saver<SheetState, Boolean>(
        save = { it.isExpanded },
        restore = { SheetState(isExpanded = it) },
    )

/**
 * Część V1 zasłonięta przez arkusz V2, jako dopełnienie od dołu; bez arkusza zero. Mapa rysuje się dalej pod
 * arkuszem, a ekran V1 przesuwa o nie tylko swoje przyciski i podpis OSM, żeby stały nad arkuszem (NFR-37).
 */
internal val LocalSheetPadding = compositionLocalOf { PaddingValues() }

/**
 * Strategia sceny dla V2: wpis z metadanymi arkusza ([metadata]) nad samym V1 rysuje się jako arkusz od dołu,
 * a V1 zostaje pod nim widoczne i żywe, z tym samym stanem (decyzje 48 i 83). W oknie co najmniej 600 dp arkusz też
 * wysuwa się od dołu (decyzja 69). Samo V1 to ta sama scena bez arkusza, więc otwarcie i zamknięcie V2 nie zmienia
 * sceny i nie uruchamia przenikania całego ekranu. Przewidywany gest „Wstecz” obsługuje sam arkusz: zjeżdża w dół
 * z postępem gestu nad niezmienioną mapą, po anulowaniu wraca na swoje miejsce, a po zakończeniu zamyka V2.
 * Inne układy historii zostawia scenie z jednym ekranem.
 */
internal object SheetSceneStrategy : SceneStrategy<NavKey> {
    private val sheet: Map<String, Any> = mapOf(SHEET_KEY to true)
    private val map: Map<String, Any> = mapOf(MAP_KEY to true)

    /** Metadane wpisu [route]: V1 to mapa pod arkuszem, V2 rysuje się jako arkusz nad V1, inne trasy bez metadanych. */
    fun metadata(route: NavKey): Map<String, Any> =
        when (route) {
            is MapRoute -> map
            is BuildingRoute -> sheet
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
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val cover =
                when {
                    sheet == null -> 0.dp
                    state.isExpanded -> maxHeight
                    else -> maxHeight * HALF_WINDOW
                }
            CompositionLocalProvider(LocalSheetPadding provides PaddingValues(bottom = cover)) { map.Content() }
            if (sheet != null) {
                SheetBackHandler { gesture ->
                    state = state.after(gesture)
                    if (gesture == BackGesture.Completed) onBack()
                }
                Sheet(
                    isExpanded = state.isExpanded,
                    onExpandedChange = { state = state.copy(isExpanded = it) },
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .height(cover)
                            .offset { IntOffset(x = 0, y = sheetBackOffset(state.progress, cover).roundToPx()) },
                ) { sheet.Content() }
            }
        }
    }
}

/**
 * Przewidywany gest „Wstecz” nad arkuszem V2: każde zdarzenie gestu (początek, postęp, anulowanie, zakończenie) trafia
 * do [onGesture], zanim zobaczy je NavDisplay, więc scena V1 pod arkuszem zostaje bez zmian.
 */
@Composable
internal fun SheetBackHandler(onGesture: (BackGesture) -> Unit) {
    val dispatcher = checkNotNull(LocalNavigationEventDispatcherOwner.current).navigationEventDispatcher
    val currentOnGesture by rememberUpdatedState(onGesture)
    DisposableEffect(dispatcher) {
        val handler =
            object : NavigationEventHandler<NavigationEventInfo.None>(NavigationEventInfo.None, isBackEnabled = true) {
                override fun onBackStarted(event: NavigationEvent) = currentOnGesture(BackGesture.Started)

                override fun onBackProgressed(event: NavigationEvent) =
                    currentOnGesture(BackGesture.Progressed(event.progress))

                override fun onBackCancelled() = currentOnGesture(BackGesture.Cancelled)

                override fun onBackCompleted() = currentOnGesture(BackGesture.Completed)
            }
        dispatcher.addHandler(handler)
        onDispose { handler.remove() }
    }
}

// Arkusz na połowę okna. Dotknięcie uchwytu albo przewinięcie treści w górę rozwija go na całe okno, więc
// przeciąganie nie jest jedyną drogą (wymagania V2). Tło `surfaceContainer`, górne rogi `Zaokraglenia.bardzoDuze`,
// cień `Wysokosci.pasekCzynnosci` padający w górę, jak w makiecie hi-fi V2.
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
                .nestedScroll(expandOnScroll),
        shape = if (isExpanded) RectangleShape else RoundedCornerShape(topStart = corner, topEnd = corner),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = Wysokosci.pasekCzynnosci,
    ) {
        // Rozwinięty arkusz sięga pod pasek stanu, zwinięty zaczyna się w połowie okna: góra treści bez wcięcia.
        val top = WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
        Column(modifier = if (isExpanded) Modifier.windowInsetsPadding(top) else Modifier.consumeWindowInsets(top)) {
            Handle(isExpanded = isExpanded, onClick = { onExpandedChange(!isExpanded) })
            Box(Modifier.weight(1f).fillMaxWidth()) { content() }
        }
    }
}

@Composable
private fun Handle(isExpanded: Boolean, onClick: () -> Unit) {
    val label = stringResource(if (isExpanded) R.string.zwin_karte else R.string.rozwin_karte)
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = CeleDotyku.minimum)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        val grip = Modifier.size(width = 32.dp, height = 4.dp)
        Box(modifier = grip.background(color = MaterialTheme.colorScheme.outline, shape = CircleShape))
    }
}
