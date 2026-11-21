/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.model.building.Building
import io.github.dostepnykampus.core.model.building.BuildingCatalog
import io.github.dostepnykampus.feature.map.barrier.ChosenBarrierSheet
import io.github.dostepnykampus.feature.map.barrier.ShownBarriers
import io.github.dostepnykampus.feature.map.building.BuildingChoiceSheet
import io.github.dostepnykampus.feature.map.building.ChoiceLink
import io.github.dostepnykampus.feature.map.camera.TouchSign
import io.github.dostepnykampus.feature.map.campus.ShownPlan
import io.github.dostepnykampus.feature.map.entrance.ChosenEntranceSheet
import io.github.dostepnykampus.feature.map.entrance.EntranceActions
import io.github.dostepnykampus.feature.map.entrance.isFullEntranceSheet
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import io.github.dostepnykampus.feature.map.overlay.SheetExits
import io.github.dostepnykampus.feature.map.plan.ChosenPlanSheet
import io.github.dostepnykampus.feature.map.plan.MapPanel
import io.github.dostepnykampus.feature.map.room.OpenRoom
import io.github.dostepnykampus.feature.map.room.RoomCard
import io.github.dostepnykampus.feature.map.room.RoomCardLayer
import io.github.dostepnykampus.feature.map.room.RoomSheetSources

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

    /** Pomieszczenie znaku planu z otwartym arkuszem; `null`, gdy otwarty arkusz nie dotyczy znaku planu. */
    val planSignId: String?
        get() = chosen?.takeIf { it.sign == TouchSign.PLAN }?.id

    /** Wysokość arkusza wejścia albo znaku planu w pikselach; 0 bez takiego arkusza. */
    var height by mutableIntStateOf(0)
        private set

    /** Symbol budynku z otwartą kartą pod mapą; `null` bez karty. */
    var card: String? = null

    /** Zamyka kartę budynku [card]. */
    var onCardClose: () -> Unit = {}

    /** Wraca do karty budynku o podanym symbolu, którą arkusz zastąpił. */
    var onCardReturn: (String) -> Unit = {}

    /** Arkusz, który zastąpił kartę, jest zamknięty, a karta jeszcze nie wróciła na ekran. */
    var isReturning by mutableStateOf(false)
        private set

    /** Wysokość karty sali na mapie w pikselach; 0 przed pomiarem. */
    var roomHeight by mutableIntStateOf(0)
        private set

    /** Sala i wysokość jej karty z drogą w pikselach z ostatniego pomiaru; `null` przed pomiarem karty z drogą. */
    var roomWay by mutableStateOf<Pair<String, Int>?>(null)
        private set

    /** Karta sali [card] na mapie ma wysokość [height] w pikselach. */
    fun measureRoom(height: Int, card: RoomCard) {
        roomHeight = height
        roomWay = (card.found.room.id to height).takeIf { card.way != null }
    }

    /** Arkusz wejścia albo znaku planu na mapie ma wysokość [height] w pikselach. */
    fun measure(height: Int) {
        this.height = height
    }

    /**
     * Wysokość w pikselach arkusza wejścia zaznaczonego w [focus], który zasłania dół mapy; 0, gdy otwarty arkusz
     * nie dotyczy tego wejścia.
     */
    fun markedCover(focus: MapFocus?): Int = height.takeIf { entranceId == (focus as? MapFocus.Entrance)?.id } ?: 0

    /** Wysokość w pikselach otwartego arkusza znaku planu, który zasłania dół mapy; 0 bez takiego arkusza. */
    fun planCover(): Int = height.takeIf { planSignId != null } ?: 0

    /**
     * Otwiera arkusz znaku [sign] obiektu [id] w miejsce karty budynku [card], zamykając ją, albo w miejsce
     * poprzedniego arkusza; arkusz po arkuszu, który zastąpił kartę, wraca do tej samej karty. [from] to symbol
     * budynku, z którego karty pokazano obiekt, gdy karty już nie ma.
     */
    fun open(sign: TouchSign, id: String, from: String? = null) {
        val under = card
        chosen = ChosenSign(sign = sign, id = id, card = under ?: from ?: chosen?.card)
        if (under != null) onCardClose()
    }

    /** Zamyka arkusz bez powrotu do karty. */
    fun close() {
        chosen = null
        height = 0
        isReturning = false
    }

    /** Zamyka arkusz i wraca przez [onCardReturn] do karty budynku, którą arkusz zastąpił. */
    fun back() {
        val card = chosen?.card
        close()
        isReturning = card != null
        card?.let(onCardReturn)
    }

    /**
     * Wiersz budynku [symbol]: arkusz, który zastąpił kartę tego budynku, wraca do niej jak [back], a inny zamyka
     * się i przekazuje symbol [onTap] jak dotknięcie konturu.
     */
    fun toBuilding(symbol: String, onTap: (String) -> Unit) {
        if (chosen?.card == symbol) {
            back()
        } else {
            close()
            onTap(symbol)
        }
    }

    /** „Zamknij” zamyka arkusz bez karty, „Wstecz” wraca do karty jak [back], a wiersz budynku jak [toBuilding]. */
    fun exits(onTap: (String) -> Unit): SheetExits =
        SheetExits(onClose = ::close, onBack = ::back, onBuilding = { toBuilding(it, onTap) })

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
 * @param onCardReturn wraca do karty budynku o podanym symbolu, którą arkusz zastąpił
 */
@Composable
internal fun rememberSignSheetState(
    focus: MapFocus?,
    cover: Dp,
    onCardClose: () -> Unit,
    onCardReturn: (String) -> Unit,
): SignSheetState {
    val state = rememberSaveable(saver = signSheetSaver) { SignSheetState(null) }
    val card = (focus as? MapFocus.Building)?.symbol.takeIf { cover > 0.dp }
    SideEffect {
        state.card = card
        state.onCardClose = onCardClose
        state.onCardReturn = onCardReturn
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
 * arkusz zajmuje cały ekran zamiast mapy; `null`, gdy takiego arkusza nie ma. [cover] to zasłonięty dół okna w px
 * w bieżącej klatce, czytany tylko w fazie układu; `null`, gdy przyciski mapy stoją nad [padding]. [panel] to arkusz
 * nad mapą: budynek, który trzyma plan, i dół mapy zasłonięty dopełnieniem kamery.
 *
 * Wymagania: US2.1, US2.8, NFR-20, US16.1
 */
internal data class ScreenSheet(
    val signs: SignSheetState,
    val layout: CardLayout,
    val padding: PaddingValues,
    val fullEntrance: String? = null,
    val cover: (() -> Int)? = null,
    val panel: MapPanel = MapPanel(),
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
 * [onCardClose], a do której arkusz wraca przez [onCardReturn]. Wejście zaznaczone z trasy ma od razu otwarty
 * swój arkusz, raz na zaznaczenie.
 *
 * Wymagania: US2.1, US2.8, US3.3
 */
@Composable
internal fun screenSheet(
    focus: MapFocus?,
    padding: PaddingValues,
    onCardClose: () -> Unit,
    onCardReturn: (String) -> Unit,
): ScreenSheet {
    val cover = padding.calculateBottomPadding()
    val signs =
        rememberSignSheetState(focus = focus, cover = cover, onCardClose = onCardClose, onCardReturn = onCardReturn)
    val marked = focus as? MapFocus.Entrance
    val entrance = marked?.id
    var isOpened by rememberSaveable(entrance) { mutableStateOf(false) }
    SideEffect {
        if (marked != null && !isOpened) {
            signs.open(sign = TouchSign.ENTRANCE, id = marked.id, from = marked.buildingSymbol)
            isOpened = true
        }
    }
    val layout = signs.cardLayout(CardLayout(padding = padding, focus = focus))
    val full = signs.entranceId?.takeIf { isFullEntranceSheet(id = it, focus = focus) }
    return ScreenSheet(signs = signs, layout = layout, padding = padding, fullEntrance = full)
}

/**
 * Arkusz wejścia na cały ekran, gdy [ScreenSheet.fullEntrance] go wskazuje; „Wstecz” i wiersz budynku wracają do
 * karty, którą arkusz zastąpił, a wiersz innego budynku otwiera jego kartę przez [onBuildingClick].
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
        exits = signs.exits(onTap = onBuildingClick),
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
 * @param onBuildingTap wiersz budynku w arkuszu bez karty pod spodem, jak dotknięcie konturu
 * @param entranceActions czynności arkusza wejścia i zgłoszenie z arkusza znaku planu
 * @param modifier położenie arkusza na mapie
 * @param hidesEntrance arkusz wejścia stoi poza mapą, na cały ekran, więc tu go nie ma
 */
@Composable
internal fun ChosenSignSheet(
    signs: SignSheetState,
    barriers: ShownBarriers,
    catalog: BuildingCatalog?,
    plan: ShownPlan?,
    onBuildingTap: (String) -> Unit,
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
                exits = signs.exits(onTap = onBuildingTap),
                modifier = modifier,
            )
        }

        TouchSign.ENTRANCE -> {
            ChosenEntranceSheet(
                id = id.takeUnless { hidesEntrance },
                catalog = catalog,
                barriers = barriers,
                exits = signs.exits(onTap = onBuildingTap),
                actions = entranceActions,
                modifier = modifier.onSizeChanged { signs.measure(it.height) },
            )
        }

        TouchSign.PLAN -> {
            val exits = signs.exits(onTap = onBuildingTap)
            ChosenPlanSheet(
                id = id,
                plan = plan,
                exits = exits,
                modifier = modifier.onSizeChanged { signs.measure(it.height) },
                onReport = entranceActions.onPlanReport,
            )
        }

        TouchSign.ROOM_STEP, TouchSign.ROOM, null -> {}
    }
}

/** Arkusz znaku z [sheet] z barierami [barriers], katalogiem [catalog] i planem [plan] na mapie. */
internal data class OpenSign(
    val sheet: ScreenSheet,
    val barriers: ShownBarriers,
    val catalog: BuildingCatalog?,
    val plan: ShownPlan?,
    val room: OpenRoom?,
)

/**
 * Arkusze V1 na dole mapy: arkusz znaku z [open] i arkusz „Który budynek?” po dotknięciu kilku budynków naraz
 * w [state]. Wybór budynku w arkuszu idzie do [onBuilding], a czynności arkusza wejścia do [entranceActions].
 *
 * Wymagania: US1.5, US2, US2.1, US3.3, US16.1
 */
@Composable
internal fun BoxScope.MapSheets(
    open: OpenSign,
    state: MapSceneState,
    onBuilding: (String) -> Unit,
    entranceActions: EntranceActions,
    onRoomHeight: (Int, RoomCard) -> Unit,
) {
    val room = open.room
    val modifier = Modifier.align(Alignment.BottomCenter).padding(Odstepy.s)
    if (room != null) {
        val sources = RoomSheetSources(onBuilding, open.barriers, entranceActions)
        RoomCardLayer(room, sources, modifier.onSizeChanged { onRoomHeight(it.height, room.card) })
        return
    }
    ChosenSignSheet(
        signs = open.sheet.signs,
        barriers = open.barriers,
        catalog = open.catalog,
        plan = open.plan,
        onBuildingTap = { state.taps.choose(it, onBuilding) },
        entranceActions = entranceActions,
        modifier = modifier,
        hidesEntrance = open.sheet.isEntranceFull,
    )
    BuildingChoices(catalog = open.catalog, state = state, onChoose = onBuilding, modifier = modifier)
}

@Composable
private fun BuildingChoices(
    catalog: BuildingCatalog?,
    state: MapSceneState,
    onChoose: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val chosen = catalog?.buildingsOf(state.taps.choice).orEmpty()
    if (chosen.isNotEmpty()) {
        BuildingChoiceSheet(onClose = state.taps::dismiss, modifier = modifier) {
            chosen.forEach { building ->
                ChoiceLink(building) { state.taps.choose(building.symbol, onChoose) }
            }
        }
    }
}

private fun BuildingCatalog.buildingsOf(symbols: List<String>): List<Building> =
    symbols.mapNotNull { symbol -> buildings.firstOrNull { it.symbol == symbol } }
