/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import io.github.dostepnykampus.app.navigation.AppNavigator
import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.map.MapListChoice
import io.github.dostepnykampus.core.data.network.NetworkStatus
import io.github.dostepnykampus.core.designsystem.BackHeader
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.feature.auth.navigation.AccountRoute
import io.github.dostepnykampus.feature.auth.navigation.SignInRoute
import io.github.dostepnykampus.feature.capture.navigation.ReportRoute
import io.github.dostepnykampus.feature.capture.navigation.ReportStep
import io.github.dostepnykampus.feature.help.HelpScreen
import io.github.dostepnykampus.feature.help.navigation.HelpRoute
import io.github.dostepnykampus.feature.map.CampusMapScreen
import io.github.dostepnykampus.feature.map.navigation.BuildingRoute
import io.github.dostepnykampus.feature.map.navigation.EntrancesRoute
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import javax.inject.Inject

private typealias Action = Pair<String, () -> Unit>

/**
 * Źródła danych ekranów z `feature:*`. W aplikacji tworzy je graf Hilt, w testach podróbki; ekrany dostają je
 * parametrem i nie wiedzą, skąd pochodzą.
 *
 * @property buildings katalog budynków dla V1 (US1)
 * @property networkStatus stan internetu dla linii „Bez internetu” (US9.1)
 * @property mapListChoice zapamiętany wybór „Mapa” albo „Lista budynków” (NFR-19)
 */
data class ScreenData
    @Inject
    constructor(
        val buildings: BuildingCatalogRepository,
        val networkStatus: NetworkStatus,
        val mapListChoice: MapListChoice,
    )

/**
 * Ekran trasy [route] z czynnościami podpiętymi do nawigatora i danymi z [data]. V1 to mapa z `feature:map`,
 * V5 instrukcja z `feature:help`, reszta to zaślepki:
 * nagłówek „Wstecz” z nazwą z listy ekranów i przyciski czynności bez danych. Ekran z `feature:*`
 * zastępuje swoją gałąź i dostaje czynności jako lambdy. Zaślepki nie zapamiętują stanu, więc nie muszą się
 * odświeżać osobno od nawigatora.
 */
@Composable
@NonRestartableComposable
internal fun AppNavigator.AppScreen(route: NavKey, data: ScreenData) {
    when (route) {
        is BuildingRoute -> BuildingStub(route.symbol)
        is EntrancesRoute -> Stub(title = stringResource(R.string.ekran_ktoredy_wejsc, route.symbol))
        is ReportRoute -> ReportStub(route.step)
        is HelpRoute -> HelpScreen(route.isFirstLaunch, onOpenMap = { backToMap() }, onLeave = { back() })
        is SignInRoute -> ToMapStub(title = R.string.nav_konto, action = R.string.nie_teraz)
        AccountRoute -> Stub(title = stringResource(if (hasWork) R.string.nav_konto_i_praca else R.string.nav_konto))
        is MapRoute -> MapScreen(focus = (route.focus as? MapFocus.Building)?.symbol, data = data)
        else -> Unit
    }
}

// V1 z obrysem budynku z trasy, np. po „Wstecz” z V2 albo „Pokaż na mapie” (US1.1, US2.1). Przyciski i podpis
// OSM stoją nad arkuszem V2 (decyzja 87).
@Composable
private fun AppNavigator.MapScreen(focus: String?, data: ScreenData) {
    CampusMapScreen(
        repository = data.buildings,
        networkStatus = data.networkStatus,
        mapListChoice = data.mapListChoice,
        onBuildingClick = { openBuilding(it) },
        focus = focus,
        contentPadding = LocalSheetPadding.current,
    )
}

@Composable
@NonRestartableComposable
private fun AppNavigator.BuildingStub(symbol: String) {
    Stub(
        symbol,
        stringResource(R.string.sprawdz_ktoredy_wejsc) to { openEntrances(symbol) },
        stringResource(R.string.pokaz_na_mapie) to { showOnMap(MapFocus.Building(symbol)) },
    )
}

@Composable
@NonRestartableComposable
private fun AppNavigator.ReportStub(step: ReportStep) {
    val action: Action =
        when (step) {
            ReportStep.PHOTO, ReportStep.GROUP -> stringResource(R.string.dalej) to { nextReportStep() }
            ReportStep.SEND -> stringResource(R.string.wyslij) to { reportSent() }
            ReportStep.SENT -> stringResource(R.string.wroc_do_mapy) to { backToMap() }
        }
    Stub(stringResource(R.string.nav_zglos_bariere), action, note = step.takeIf { it != ReportStep.SENT })
}

@Composable
@NonRestartableComposable
private fun AppNavigator.ToMapStub(title: Int, action: Int) {
    Stub(stringResource(title), stringResource(action) to { backToMap() })
}

@Composable
@NonRestartableComposable
private fun AppNavigator.Stub(title: String, vararg actions: Action, note: ReportStep? = null) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            BackHeader(title = title, onBack = { back() })
            note?.let { Text(stringResource(R.string.krok_z_3, it.ordinal + 1), Modifier.padding(Odstepy.m)) }
            actions.forEach { (label, onClick) -> TextButton(onClick = onClick) { Text(text = label) } }
        }
    }
}
