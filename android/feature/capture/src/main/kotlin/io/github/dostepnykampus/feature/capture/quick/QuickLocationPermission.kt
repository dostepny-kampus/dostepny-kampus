/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.quick

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.core.designsystem.bar.PinnedButton
import io.github.dostepnykampus.core.designsystem.bar.PinnedButtonKind
import io.github.dostepnykampus.core.designsystem.card.InfoPanel
import io.github.dostepnykampus.feature.capture.R
import io.github.dostepnykampus.feature.capture.location.LocationPermissionState
import io.github.dostepnykampus.feature.capture.location.locationSettings

/**
 * Udostępnia odpowiednią czynność systemową dla [state] oraz [onBackToMap].
 *
 * Wymagania: UC7, NFR-27, BR-8, BR-14
 */
@Composable
internal fun QuickLocationPermission(
    state: LocationPermissionState,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
    onBackToMap: () -> Unit,
) {
    val notice =
        when (state) {
            LocationPermissionState.MISSING -> LocationNotice.MISSING
            LocationPermissionState.APPROXIMATE -> LocationNotice.APPROXIMATE
            LocationPermissionState.DENIED -> LocationNotice.DENIED
        }
    val action = if (state == LocationPermissionState.DENIED) onOpenSettings else onAllow
    QuickLocationNotice(notice, action, onBackToMap)
}

/** Udostępnia ustawienia dostawców lokalizacji oraz [onBackToMap]. Wymagania: UC7 */
@Composable
internal fun QuickLocationOff(onBackToMap: () -> Unit) {
    val context = LocalContext.current
    QuickLocationNotice(LocationNotice.OFF, { context.startActivity(locationSettings()) }, onBackToMap)
}

@Composable
private fun QuickLocationNotice(notice: LocationNotice, onAction: () -> Unit, onBackToMap: () -> Unit) {
    InfoPanel {
        Text(
            text = stringResource(notice.summaryRes),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = Typografia.wyroznienieWaga,
        )
        Text(stringResource(notice.detailRes))
        Column(
            modifier = Modifier.padding(top = Odstepy.s),
            verticalArrangement = Arrangement.spacedBy(Odstepy.m),
        ) {
            PinnedButton(
                text = stringResource(notice.actionRes),
                onClick = onAction,
                modifier = Modifier.fillMaxWidth(),
            )
            PinnedButton(
                text = stringResource(R.string.szybkie_wroc_do_mapy),
                onClick = onBackToMap,
                modifier = Modifier.fillMaxWidth(),
                kind = PinnedButtonKind.Outlined,
            )
        }
    }
}

private enum class LocationNotice(
    @param:StringRes val summaryRes: Int,
    @param:StringRes val detailRes: Int,
    @param:StringRes val actionRes: Int,
) {
    MISSING(R.string.szybkie_zgoda_tekst, R.string.szybkie_zgoda_zapewnienie, R.string.pomiar_zgoda_przycisk),
    APPROXIMATE(
        R.string.szybkie_zgoda_przyblizona,
        R.string.szybkie_zgoda_dokladna,
        R.string.pomiar_zgoda_dokladna_przycisk,
    ),
    DENIED(R.string.szybkie_zgoda_odmowa, R.string.szybkie_zgoda_ustawienia, R.string.pomiar_otworz_ustawienia),
    OFF(R.string.szybkie_lokalizacja_wylaczona, R.string.szybkie_lokalizacja_wlacz, R.string.pomiar_otworz_ustawienia),
}
