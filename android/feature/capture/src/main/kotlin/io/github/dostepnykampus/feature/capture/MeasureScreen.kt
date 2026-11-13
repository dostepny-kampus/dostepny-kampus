/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import android.content.Intent
import android.content.res.Resources
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dostepnykampus.core.designsystem.BackHeader
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.PinnedActionBar
import io.github.dostepnykampus.core.designsystem.PinnedButton
import io.github.dostepnykampus.core.designsystem.PinnedButtonKind
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.core.location.GnssSystem
import io.github.dostepnykampus.core.location.PositionAverage
import io.github.dostepnykampus.feature.capture.navigation.ReportFormRoute
import java.util.Locale

private val POLISH = Locale.forLanguageTag("pl-PL")

/**
 * C1 Pomiar pozycji: szukanie satelitów, pomiar 30 s i „Pomiar zakończony”. Dokładność i satelity są
 * tekstem, nie tylko na mapie nieba, a dokładność ma dopisek „szacunek”. „Powtórz pomiar”
 * działa w każdym stanie, bez limitu. Bez zgody na lokalizację pokazuje prośbę o nią, a przy
 * wyłączonej lokalizacji w telefonie „Otwórz ustawienia” lokalizacji.
 * Po pomiarze podpowiada budynek, a po decyzji o budynku „Dalej” otwiera C2.
 *
 * Wymagania: US6.1, NFR-18, US6.5, NFR-2, NFR-23, NFR-27, UC7, US6.2, US6, NFR-15
 *
 * @param onBack „Wstecz” do ekranu, z którego otwarto C1
 * @param onNext „Dalej” z trasą C2 i miejscem z pomiaru
 * @param modifier wymiary nadane przez aplikację
 * @param viewModel pomiar; w aplikacji z grafu Hilt, w testach ze źródłami testowymi
 */
@Composable
fun MeasureScreen(
    onBack: () -> Unit,
    onNext: (ReportFormRoute) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MeasureViewModel = hiltViewModel(),
) {
    WithLocationPermission(onBack = onBack) {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val buildings by viewModel.buildings.collectAsStateWithLifecycle()
        val isRestarted by viewModel.restarted.collectAsStateWithLifecycle()
        MeasureContent(
            state = state,
            isRestarted = isRestarted,
            onRepeat = viewModel::repeat,
            onBack = onBack,
            modifier = modifier,
            onNext = onNext,
            buildings = Buildings(buildings),
            placeActions = PlaceActions(viewModel::confirm, viewModel::choose, viewModel::outside),
        )
    }
}

/**
 * Treść C1 dla stanu [state], bez ViewModelu.
 *
 * Wymagania: US6.2, NFR-23, NFR-27
 *
 * @param state stan pomiaru
 * @param onRepeat „Powtórz pomiar”
 * @param onBack „Wstecz”
 * @param modifier wymiary nadane przez aplikację
 * @param onNext „Dalej”, widoczne po pomiarze i decyzji o budynku
 * @param buildings budynki do „Wybierz inny”
 * @param placeActions czynności wyboru budynku po pomiarze
 * @param isRestarted pomiar przerwano i zaczął się od nowa
 */
@Composable
fun MeasureContent(
    state: MeasureUiState,
    onRepeat: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onNext: (ReportFormRoute) -> Unit = {},
    buildings: Buildings = Buildings(emptyList()),
    placeActions: PlaceActions = PlaceActions.None,
    isRestarted: Boolean = false,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            BackHeader(title = stringResource(R.string.pomiar_naglowek), onBack = onBack)
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(Odstepy.m)) {
                MeasureBody(
                    state = state,
                    buildings = buildings,
                    placeActions = placeActions,
                    isRestarted = isRestarted,
                )
            }
            MeasureActions(state = state, onRepeat = onRepeat, onNext = onNext)
        }
    }
}

@Composable
private fun MeasureBody(
    state: MeasureUiState,
    buildings: Buildings,
    placeActions: PlaceActions,
    isRestarted: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        if (isRestarted && (state == MeasureUiState.Searching || state is MeasureUiState.Measuring)) {
            Text(
                text = stringResource(R.string.pomiar_od_nowa),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        val resources = LocalResources.current
        when (state) {
            MeasureUiState.Searching -> {
                Text(
                    text = stringResource(R.string.pomiar_szukanie),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }

            MeasureUiState.LocationOff -> {
                Notice(stringResource(R.string.pomiar_lokalizacja_wylaczona))
            }

            MeasureUiState.Failed -> {
                Notice(stringResource(R.string.pomiar_blad))
            }

            is MeasureUiState.Measuring -> {
                Headline(stringResource(R.string.pomiar_zostalo, state.remainingSeconds))
                Result(state.average, resources.satellitesLine(state.satellites))
                SkyMap(Sky(state.sky))
            }

            is MeasureUiState.Finished -> {
                Headline(stringResource(R.string.pomiar_zakonczony), isAnnounced = true)
                Result(state.average, resources.satellitesLine(state.satellites))
                PlaceSection(place = state.place, buildings = buildings, actions = placeActions)
                SkyMap(Sky(state.sky))
            }
        }
    }
}

@Composable
private fun MeasureActions(state: MeasureUiState, onRepeat: () -> Unit, onNext: (ReportFormRoute) -> Unit) {
    val context = LocalContext.current
    PinnedActionBar {
        if (state == MeasureUiState.LocationOff) {
            PinnedButton(
                text = stringResource(R.string.pomiar_otworz_ustawienia),
                onClick = { context.startActivity(locationSettings()) },
            )
        } else {
            val next = state.reportFormRoute()
            val isWaiting = state == MeasureUiState.Searching || state == MeasureUiState.Failed
            val kind = if (isWaiting) PinnedButtonKind.Filled else PinnedButtonKind.Outlined
            PinnedButton(text = stringResource(R.string.pomiar_powtorz), onClick = onRepeat, kind = kind)
            next?.let { PinnedButton(text = stringResource(R.string.pomiar_dalej), onClick = { onNext(it) }) }
        }
    }
}

@Composable
private fun Notice(text: String) {
    Text(
        text = text,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        color = MaterialTheme.colorScheme.error,
        fontWeight = Typografia.wyroznienieWaga,
    )
}

private fun locationSettings(): Intent =
    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

@Composable
private fun Headline(text: String, isAnnounced: Boolean = false) {
    Text(
        text = text,
        modifier =
            Modifier.semantics {
                heading()
                if (isAnnounced) liveRegion = LiveRegionMode.Polite
            },
        style = MaterialTheme.typography.titleMedium,
        fontWeight = Typografia.wyroznienieWaga,
    )
}

@Composable
private fun Result(average: PositionAverage, satellitesLine: String) {
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        Text(stringResource(R.string.pomiar_dokladnosc, String.format(POLISH, "%.1f", average.accuracyMeters)))
        Text(satellitesLine)
    }
}

/** „Satelity: GPS 7, Galileo 5, GLONASS 4” w kolejności systemów. Wymagania: US6.1 */
internal fun Resources.satellitesLine(satellites: Map<GnssSystem, Int>): String {
    if (satellites.isEmpty()) return getString(R.string.pomiar_satelity_brak)
    return getString(R.string.pomiar_satelity, systemCounts(satellites))
}

/** „GPS 7, Galileo 5” w kolejności mapy [satellites]. */
internal fun Resources.systemCounts(satellites: Map<GnssSystem, Int>): String =
    satellites.entries.joinToString(", ") { (system, count) -> "${systemName(system)} $count" }

/** Nazwa systemu GNSS z napisów modułu. */
internal fun Resources.systemName(system: GnssSystem): String =
    getString(
        when (system) {
            GnssSystem.GPS -> R.string.system_gps
            GnssSystem.GALILEO -> R.string.system_galileo
            GnssSystem.GLONASS -> R.string.system_glonass
            GnssSystem.BEIDOU -> R.string.system_beidou
            GnssSystem.QZSS -> R.string.system_qzss
            GnssSystem.NAVIC -> R.string.system_navic
            GnssSystem.SBAS -> R.string.system_sbas
            GnssSystem.UNKNOWN -> R.string.system_inny
        },
    )
