/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.help

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.designsystem.BackHeader
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.MarkingChip
import io.github.dostepnykampus.core.designsystem.Obramowania
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.PinnedActionBar
import io.github.dostepnykampus.core.designsystem.PinnedButton
import io.github.dostepnykampus.core.designsystem.PinnedButtonKind.Outlined
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.core.model.EntranceMarking

private const val LARGE_FONT_SCALE = 1.5f

/**
 * Ekran V5 „Pomoc” (US5.1): instrukcja krok po kroku, przejście przyciskiem „Dalej” zamiast przesunięcia
 * palcem (NFR-53), bez automatycznego przewijania (NFR-23). Krok jest stanem zapisywanym, a ponowne
 * dotknięcie „Pomoc” zaczyna od kroku 1 (decyzja 72). Przy skali czcionki co najmniej 1,5 czynności stoją na
 * końcu treści zamiast w przypiętym pasku (reguła 10 dużej czcionki).
 *
 * @param isFirstLaunch instrukcja otworzyła się sama; „Wstecz” w kroku 1 działa wtedy jak „Pomiń instrukcję”
 * @param onOpenMap „Pomiń instrukcję”, „Zacznij” w ostatnim kroku i „Wstecz” w kroku 1 przy pierwszym uruchomieniu
 * @param onLeave „Wstecz” w kroku 1 po otwarciu z paska: powrót do ekranu, z którego otwarto V5
 * @param modifier wymiary ekranu nadane przez aplikację
 */
@Composable
fun HelpScreen(isFirstLaunch: Boolean, onOpenMap: () -> Unit, onLeave: () -> Unit, modifier: Modifier = Modifier) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val isLast = step == HelpSteps.lastIndex
    val onBack: () -> Unit = {
        when (helpBack(step, isFirstLaunch)) {
            HelpBack.PREVIOUS_STEP -> step--
            HelpBack.SKIP -> onOpenMap()
            HelpBack.LEAVE -> onLeave()
        }
    }
    val onNext: () -> Unit = { if (isLast) onOpenMap() else step++ }
    BackHandler(enabled = step > 0, onBack = onBack)
    val isLargeFont = LocalDensity.current.fontScale >= LARGE_FONT_SCALE
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            BackHeader(title = stringResource(R.string.pomoc_tytul), onBack = onBack)
            val actions = @Composable {
                NextAndSkipButtons(isLast = isLast, onNext = onNext, onOpenMap = onOpenMap, isInContent = isLargeFont)
            }
            key(step) {
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(Odstepy.m),
                    verticalArrangement = Arrangement.spacedBy(Odstepy.m),
                ) {
                    StepNumberAndBar(step = step)
                    StepTitleTextAndImage(HelpSteps[step])
                    if (isLargeFont) actions()
                }
            }
            if (!isLargeFont) actions()
        }
    }
}

@Composable
private fun StepNumberAndBar(step: Int) {
    // Pasek z odcinków tylko powtarza napis „Krok 2 z 3”, więc TalkBack go nie czyta (NFR-21).
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        Text(
            text = stringResource(R.string.pomoc_krok, step + 1, HelpSteps.size),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = Typografia.wyroznienieWaga,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Odstepy.s)) {
            HelpSteps.indices.forEach { index ->
                val color = if (index <= step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                val segment = Modifier.weight(1f).height(Odstepy.s).border(Obramowania.pole, color, CircleShape)
                Spacer(modifier = if (index <= step) segment.background(color, CircleShape) else segment)
            }
        }
    }
}

@Composable
private fun StepTitleTextAndImage(content: HelpStep) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Column(Modifier.fillMaxWidth().padding(Odstepy.m), verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
            Text(
                text = stringResource(content.title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
            )
            Text(text = stringResource(content.text), style = MaterialTheme.typography.bodyLarge)
            content.example?.let { example ->
                Text(
                    text = stringResource(example.caption),
                    modifier = Modifier.padding(top = Odstepy.s),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                // Obraz w instrukcji: TalkBack czyta jeden opis zamiast napisów przykładu (NFR-18).
                val description = stringResource(example.description)
                val image = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description }
                when (example) {
                    HelpExample.SEARCH_FIELD -> SearchFieldExample(image)
                    HelpExample.ENTRANCE -> EntranceExample(image)
                }
            }
        }
    }
}

@Composable
private fun SearchFieldExample(modifier: Modifier = Modifier) {
    // Pole wyszukiwania V1 jako obraz: ten sam kształt i podpowiedź, bez lupy i bez możliwości wpisania.
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(Obramowania.pole, MaterialTheme.colorScheme.outline),
    ) {
        Text(
            text = stringResource(R.string.pomoc_budynek_pole),
            modifier =
                Modifier
                    .heightIn(min = CeleDotyku.przycisk)
                    .wrapContentHeight()
                    .padding(horizontal = Odstepy.m, vertical = Odstepy.s),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun EntranceExample(modifier: Modifier = Modifier) {
    // Wejście z opisu budynku na tle kroku: oznaczenie „Dostępne”, nazwa wejścia i szerokość drzwi.
    val frame = modifier.border(Obramowania.ramka, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
    Column(modifier = frame.padding(Odstepy.m), verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        MarkingChip(marking = EntranceMarking.ACCESSIBLE)
        Text(text = stringResource(R.string.pomoc_dostepne_wejscie), style = MaterialTheme.typography.titleMedium)
        Text(text = stringResource(R.string.pomoc_dostepne_drzwi), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun NextAndSkipButtons(isLast: Boolean, onNext: () -> Unit, onOpenMap: () -> Unit, isInContent: Boolean) {
    val next = stringResource(if (isLast) R.string.pomoc_zacznij else R.string.pomoc_dalej)
    val skip = stringResource(R.string.pomoc_pomin)
    val buttons: @Composable (Modifier) -> Unit = { width ->
        PinnedButton(text = next, onClick = onNext, modifier = width)
        PinnedButton(text = skip, onClick = onOpenMap, modifier = width, kind = Outlined)
    }
    if (isInContent) {
        Column(verticalArrangement = Arrangement.spacedBy(CeleDotyku.odstep)) { buttons(Modifier.fillMaxWidth()) }
    } else {
        PinnedActionBar { buttons(Modifier.weight(1f)) }
    }
}
