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
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.Wysokosci
import io.github.dostepnykampus.core.designsystem.Zaokraglenia
import io.github.dostepnykampus.feature.map.navigation.BuildingRoute

private const val SHEET_KEY = "arkusz"
private const val HALF_WINDOW = 0.5f

/**
 * Część V1 zasłonięta przez arkusz V2, jako dopełnienie od dołu; bez arkusza zero. Mapa rysuje się dalej pod
 * arkuszem, a ekran V1 przesuwa o nie tylko swoje przyciski i podpis OSM, żeby stały nad arkuszem (NFR-37).
 */
internal val LocalSheetPadding = compositionLocalOf { PaddingValues() }

/**
 * Strategia sceny dla V2: wpis z metadanymi arkusza ([metadata]) nad samym V1 rysuje się jako arkusz od dołu,
 * a V1 zostaje pod nim widoczne i żywe, z tym samym stanem (decyzje 48 i 83). W oknie co najmniej 600 dp arkusz też
 * wysuwa się od dołu (decyzja 69). Inne układy historii zostawia scenie z jednym ekranem.
 */
internal object SheetSceneStrategy : SceneStrategy<NavKey> {
    private val sheet: Map<String, Any> = mapOf(SHEET_KEY to true)

    /** Metadane wpisu [route]: V2 rysuje się jako arkusz nad V1, inne trasy bez metadanych. */
    fun metadata(route: NavKey): Map<String, Any> = if (route is BuildingRoute) sheet else emptyMap()

    override fun SceneStrategyScope<NavKey>.calculateScene(entries: List<NavEntry<NavKey>>): Scene<NavKey>? {
        val isSheetOverMap = entries.size == 2 && SHEET_KEY in entries.last().metadata
        return if (isSheetOverMap) SheetScene(map = entries.first(), sheet = entries.last()) else null
    }
}

private data class SheetScene(val map: NavEntry<NavKey>, val sheet: NavEntry<NavKey>) : Scene<NavKey> {
    override val key: Any = map.contentKey
    override val entries: List<NavEntry<NavKey>> = listOf(map, sheet)
    override val previousEntries: List<NavEntry<NavKey>> = listOf(map)
    override val content: @Composable () -> Unit = {
        var isExpanded by rememberSaveable { mutableStateOf(false) }
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val cover = if (isExpanded) maxHeight else maxHeight * HALF_WINDOW
            CompositionLocalProvider(LocalSheetPadding provides PaddingValues(bottom = cover)) { map.Content() }
            Sheet(
                isExpanded = isExpanded,
                onExpandedChange = { isExpanded = it },
                modifier = Modifier.align(Alignment.BottomCenter).height(cover),
            ) { sheet.Content() }
        }
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
            content()
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
