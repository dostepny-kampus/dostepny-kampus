/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.model.building.BuildingCatalog
import io.github.dostepnykampus.feature.map.barrier.ChosenBarrierSheet
import io.github.dostepnykampus.feature.map.barrier.ShownBarriers
import io.github.dostepnykampus.feature.map.camera.TouchSign
import io.github.dostepnykampus.feature.map.entrance.ChosenEntranceSheet
import io.github.dostepnykampus.feature.map.entrance.EntranceActions
import io.github.dostepnykampus.feature.map.entrance.isFullEntranceSheet
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import io.github.dostepnykampus.feature.map.plan.ChosenPlanSheet
import io.github.dostepnykampus.feature.map.plan.ShownPlan

/**
 * Znak [sign] obiektu o identyfikatorze [id] dotknięty na mapie V1, z otwartym arkuszem; [card] to symbol budynku,
 * którego kartę arkusz zastąpił, a `null` bez karty.
 *
 * Wymagania: US1.5, US2.1
 */
internal data class ChosenSign(val sign: TouchSign, val id: String, val card: String? = null)

/**
 * Arkusz znaku na mapie V1: który znak ma otwarty arkusz.
 *
 * Wymagania: US1.5, US2.1
 *
 * @param initial znak otwarty od początku; `null` bez arkusza
 */
@Stable
internal class SignSheetState(initial: ChosenSign?) {
    /** Znak z otwartym arkuszem; `null` bez arkusza. */
    var chosen by mutableStateOf(initial)
        private set

    /** Identyfikator bariery z otwartym arkuszem; `null`, gdy otwarty arkusz nie dotyczy bariery. */
    val barrierId: String?
        get() = chosen?.takeIf { it.sign == TouchSign.BARRIER }?.id

    /** Identyfikator wejścia z otwartym arkuszem; `null`, gdy otwarty arkusz nie dotyczy wejścia. */
    val entranceId: String?
        get() = chosen?.takeIf { it.sign == TouchSign.ENTRANCE }?.id

    /** Symbol budynku z otwartą kartą pod mapą; `null` bez karty. */
    var card: String? = null

    /** Zamyka kartę budynku [card]. */
    var onCardClose: () -> Unit = {}

    /** Arkusz, który zastąpił kartę, jest zamknięty, a karta jeszcze nie wróciła na ekran. */
    var isReturning by mutableStateOf(false)
        private set

    /**
     * Otwiera arkusz znaku [sign] obiektu [id] w miejsce karty budynku [card], zamykając ją, albo w miejsce
     * poprzedniego arkusza; arkusz po arkuszu, który zastąpił kartę, wraca do tej samej karty.
     */
    fun open(sign: TouchSign, id: String) {
        val under = card
        chosen = ChosenSign(sign = sign, id = id, card = under ?: chosen?.card)
        if (under != null) onCardClose()
    }

    /** Zamyka arkusz bez powrotu do karty. */
    fun close() {
        chosen = null
        isReturning = false
    }

    /** Zamyka arkusz i przekazuje [onCard] symbol budynku, którego kartę arkusz zastąpił. */
    fun back(onCard: (String) -> Unit) {
        val card = chosen?.card
        close()
        isReturning = card != null
        card?.let(onCard)
    }

    /** Karta budynku wróciła na ekran. */
    fun cardShown() {
        isReturning = false
    }
}

private val signSheetSaver: Saver<SignSheetState, List<String>> =
    Saver(
        save = { state -> state.chosen?.run { listOfNotNull(sign.name, id, card) } },
        restore = { saved ->
            SignSheetState(ChosenSign(sign = TouchSign.valueOf(saved[0]), id = saved[1], card = saved.getOrNull(2)))
        },
    )

/**
 * Stan arkusza znaku zachowany przy zmianie konfiguracji; na początku bez arkusza.
 *
 * @param focus zaznaczenie z trasy; budynek z niego ma kartę, gdy [cover] jest większe od zera
 * @param cover wysokość karty budynku zasłaniającej mapę od dołu
 * @param onCardClose zamyka kartę budynku
 */
@Composable
internal fun rememberSignSheetState(focus: MapFocus?, cover: Dp, onCardClose: () -> Unit): SignSheetState {
    val state = rememberSaveable(saver = signSheetSaver) { SignSheetState(null) }
    val card = (focus as? MapFocus.Building)?.symbol.takeIf { cover > 0.dp }
    SideEffect {
        state.card = card
        state.onCardClose = onCardClose
    }
    return state
}

/**
 * Zasłonięcie mapy przez kartę budynku [padding] i zaznaczenie z trasy [focus].
 *
 * Wymagania: US2.1, US2.8
 */
internal data class CardLayout(val padding: PaddingValues, val focus: MapFocus?)

/**
 * Układ karty budynku dla kamery i ekranu: przy arkuszu, który zastąpił kartę, i do powrotu karty zostaje układ
 * karty sprzed arkusza, czyli jej zasłonięcie i zaznaczony budynek; poza tym [now].
 *
 * Wymagania: US2.1, US2.8, NFR-17
 */
@Composable
internal fun SignSheetState.cardLayout(now: CardLayout): CardLayout {
    val isHeld = chosen?.card != null || (isReturning && now.padding.calculateBottomPadding() == 0.dp)
    var last by remember { mutableStateOf(now) }
    SideEffect {
        if (!isHeld) {
            last = now
            cardShown()
        }
    }
    return if (isHeld) last else now
}

/**
 * Arkusz znaku [signs] nad mapą i zasłonięcie ekranu przez kartę budynku: [padding] rzeczywiste, a [layout]
 * z zasłonięciem i zaznaczeniem dla kamery i układu ekranu według [cardLayout]. [fullEntrance] to wejście, którego
 * arkusz zajmuje cały ekran zamiast mapy; `null`, gdy takiego arkusza nie ma.
 *
 * Wymagania: US2.1, US2.8, NFR-20
 */
internal data class ScreenSheet(
    val signs: SignSheetState,
    val layout: CardLayout,
    val padding: PaddingValues,
    val fullEntrance: String? = null,
) {
    /** Arkusz wejścia zajmuje cały ekran zamiast mapy. */
    val isEntranceFull: Boolean
        get() = fullEntrance != null

    /** Karta budynku zasłania teraz mapę. */
    val isCardOpen: Boolean
        get() = padding.calculateBottomPadding() > 0.dp
}

/**
 * Arkusz znaku przy zaznaczeniu [focus] i zasłonięciu ekranu [padding] przez kartę budynku, którą zamyka
 * [onCardClose]. Wejście zaznaczone z trasy ma od razu otwarty swój arkusz, raz na zaznaczenie.
 *
 * Wymagania: US2.1, US2.8, US3.3
 */
@Composable
internal fun screenSheet(focus: MapFocus?, padding: PaddingValues, onCardClose: () -> Unit): ScreenSheet {
    val cover = padding.calculateBottomPadding()
    val signs = rememberSignSheetState(focus = focus, cover = cover, onCardClose = onCardClose)
    val entrance = (focus as? MapFocus.Entrance)?.id
    var isOpened by rememberSaveable(entrance) { mutableStateOf(false) }
    SideEffect {
        if (entrance != null && !isOpened) {
            signs.open(TouchSign.ENTRANCE, entrance)
            isOpened = true
        }
    }
    val layout = signs.cardLayout(CardLayout(padding = padding, focus = focus))
    val full = signs.entranceId?.takeIf { isFullEntranceSheet(id = it, focus = focus) }
    return ScreenSheet(signs = signs, layout = layout, padding = padding, fullEntrance = full)
}

/**
 * Arkusz wejścia na cały ekran, gdy [ScreenSheet.fullEntrance] go wskazuje; zamknięcie wraca do karty, którą arkusz
 * zastąpił, przez [onBuildingClick].
 *
 * Wymagania: NFR-19, NFR-20, NFR-51
 *
 * @param ui stan mapy z katalogiem budynków i wejść oraz barierami ze zgłoszeń przyjętych
 * @param actions czynności arkusza wejścia
 * @param onBuildingClick V2 budynku o podanym symbolu
 */
@Composable
internal fun ScreenSheet.FullEntranceSheet(
    ui: CampusMapUiState,
    actions: EntranceActions,
    onBuildingClick: (String) -> Unit,
) {
    ChosenEntranceSheet(
        id = fullEntrance,
        catalog = ui.catalog,
        barriers = ShownBarriers(ui.barriers),
        onClose = { signs.back(onBuildingClick) },
        actions = actions,
        isFull = true,
    )
}

/**
 * Arkusz znaku z [signs]: bariera spośród [barriers], wejście z [catalog] albo znak [plan]; bez znaku arkusza
 * nie ma.
 *
 * Wymagania: US1.5, US2.1, US3.3, US16.1
 *
 * @param signs znak z otwartym arkuszem; zamknięcie arkusza go zdejmuje i wraca do karty, którą arkusz zastąpił
 * @param barriers bariery pokazane na mapie
 * @param catalog katalog budynków i wejść; `null` przed jego wczytaniem
 * @param plan plan piętra na mapie; `null`, gdy planu nie widać
 * @param onBuildingClick V2 budynku o podanym symbolu, także powrót do karty
 * @param entranceActions czynności arkusza wejścia
 * @param modifier położenie arkusza na mapie
 * @param hidesEntrance arkusz wejścia stoi poza mapą, na cały ekran, więc tu go nie ma
 */
@Composable
internal fun ChosenSignSheet(
    signs: SignSheetState,
    barriers: ShownBarriers,
    catalog: BuildingCatalog?,
    plan: ShownPlan?,
    onBuildingClick: (String) -> Unit,
    entranceActions: EntranceActions,
    modifier: Modifier = Modifier,
    hidesEntrance: Boolean = false,
) {
    val id = signs.chosen?.id
    when (signs.chosen?.sign) {
        TouchSign.BARRIER -> {
            ChosenBarrierSheet(
                id = id,
                barriers = barriers,
                catalog = catalog,
                onClose = { signs.back(onBuildingClick) },
                onBuildingClick = onBuildingClick,
                modifier = modifier,
            )
        }

        TouchSign.ENTRANCE -> {
            ChosenEntranceSheet(
                id = id.takeUnless { hidesEntrance },
                catalog = catalog,
                barriers = barriers,
                onClose = { signs.back(onBuildingClick) },
                actions = entranceActions,
                modifier = modifier,
            )
        }

        TouchSign.PLAN -> {
            ChosenPlanSheet(id = id, plan = plan, onClose = { signs.back(onBuildingClick) }, modifier = modifier)
        }

        null -> {}
    }
}
