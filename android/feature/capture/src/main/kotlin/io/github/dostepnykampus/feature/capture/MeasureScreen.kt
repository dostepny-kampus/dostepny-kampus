/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import android.content.res.Resources
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
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
import java.util.Locale

private val POLISH = Locale.forLanguageTag("pl-PL")

/**
 * C1 Pomiar pozycji (US6.1): szukanie satelitów, pomiar 30 s i „Pomiar zakończony”. Dokładność i satelity są
 * tekstem, nie tylko wykresem (NFR-18), a dokładność ma dopisek „szacunek” (NFR-2). „Powtórz pomiar” działa w każdym
 * stanie, bez limitu (NFR-23). Bez zgody na lokalizację pokazuje prośbę o nią (NFR-27). Podpowiedź budynku, mapa
 * nieba i „Dalej” przyjdą w kolejnych krokach.
 *
 * @param onBack „Wstecz” do ekranu, z którego otwarto C1 (NFR-15)
 * @param modifier wymiary nadane przez aplikację
 * @param viewModel pomiar; w aplikacji z grafu Hilt, w testach ze źródłami testowymi
 */
@Composable
fun MeasureScreen(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: MeasureViewModel = hiltViewModel()) {
    WithLocationPermission(onBack = onBack) {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        MeasureContent(state = state, onRepeat = viewModel::repeat, onBack = onBack, modifier = modifier)
    }
}

/**
 * Treść C1 dla stanu [state], bez ViewModelu.
 *
 * @param state stan pomiaru
 * @param onRepeat „Powtórz pomiar”
 * @param onBack „Wstecz”
 * @param modifier wymiary nadane przez aplikację
 */
@Composable
fun MeasureContent(state: MeasureUiState, onRepeat: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            BackHeader(title = stringResource(R.string.pomiar_naglowek), onBack = onBack)
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(Odstepy.m),
                verticalArrangement = Arrangement.spacedBy(Odstepy.s),
            ) {
                val resources = LocalResources.current
                when (state) {
                    MeasureUiState.Searching -> {
                        Text(stringResource(R.string.pomiar_szukanie))
                    }

                    is MeasureUiState.Measuring -> {
                        Headline(stringResource(R.string.pomiar_zostalo, state.remainingSeconds))
                        Result(state.average, resources.satellitesLine(state.satellites))
                    }

                    is MeasureUiState.Finished -> {
                        Headline(stringResource(R.string.pomiar_zakonczony))
                        Result(state.average, resources.satellitesLine(state.satellites))
                    }
                }
            }
            PinnedActionBar {
                val kind = if (state == MeasureUiState.Searching) PinnedButtonKind.Filled else PinnedButtonKind.Outlined
                PinnedButton(text = stringResource(R.string.pomiar_powtorz), onClick = onRepeat, kind = kind)
            }
        }
    }
}

@Composable
private fun Headline(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium, fontWeight = Typografia.wyroznienieWaga)
}

@Composable
private fun Result(average: PositionAverage, satellitesLine: String) {
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        Text(stringResource(R.string.pomiar_dokladnosc, String.format(POLISH, "%.1f", average.accuracyMeters)))
        Text(satellitesLine)
    }
}

/** „Satelity: GPS 7, Galileo 5, GLONASS 4” w kolejności systemów (US6.1). */
internal fun Resources.satellitesLine(satellites: Map<GnssSystem, Int>): String {
    if (satellites.isEmpty()) return getString(R.string.pomiar_satelity_brak)
    val parts = satellites.map { (system, count) -> "${systemName(system)} $count" }
    return getString(R.string.pomiar_satelity, parts.joinToString(", "))
}

private fun Resources.systemName(system: GnssSystem): String =
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
