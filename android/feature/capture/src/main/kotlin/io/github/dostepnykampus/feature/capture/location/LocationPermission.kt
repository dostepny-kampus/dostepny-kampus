/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.location

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.bar.AdaptiveScreen
import io.github.dostepnykampus.core.designsystem.bar.BackHeader
import io.github.dostepnykampus.core.designsystem.bar.PinnedActionBar
import io.github.dostepnykampus.core.designsystem.bar.PinnedButton
import io.github.dostepnykampus.feature.capture.R

private val LOCATION = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

/** Czy aplikacja ma zgodę na dokładną lokalizację; przybliżona nie wystarczy do pomiaru. Wymagania: US6.1 */
internal fun Context.hasFineLocation(): Boolean =
    checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

/** Czy aplikacja ma zgodę na przybliżoną lokalizację. Wymagania: BR-8, US6.1 */
internal fun Context.hasCoarseLocation(): Boolean =
    checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

/**
 * Stan prośby o zgodę na lokalizację albo `null`, gdy aplikacja ma zgodę na dokładną lokalizację.
 *
 * Wymagania: NFR-27, BR-8, US6.1
 *
 * @param isDenied czy użytkownik wcześniej odmówił zgody
 */
internal fun Context.permissionNeed(isDenied: Boolean): LocationPermissionState? =
    when {
        hasFineLocation() -> null
        hasCoarseLocation() -> LocationPermissionState.APPROXIMATE
        isDenied -> LocationPermissionState.DENIED
        else -> LocationPermissionState.MISSING
    }

/**
 * Stan braku dokładnej zgody na lokalizację na ekranie prośby.
 *
 * Wymagania: NFR-27, BR-8, US6.1
 */
enum class LocationPermissionState(
    @param:StringRes internal val messageRes: Int,
    @param:StringRes internal val actionRes: Int,
) {
    /** Brak zgody na lokalizację przed systemowym pytaniem. */
    MISSING(R.string.pomiar_zgoda_tekst, R.string.pomiar_zgoda_przycisk),

    /** Zgoda tylko na przybliżoną lokalizację: prośba o zmianę na dokładną. */
    APPROXIMATE(R.string.pomiar_zgoda_przyblizona, R.string.pomiar_zgoda_dokladna_przycisk),

    /** Odmowa zgody: przejście do ustawień telefonu. */
    DENIED(R.string.pomiar_zgoda_odmowa, R.string.pomiar_otworz_ustawienia),
}

/**
 * Pokazuje [content] dopiero ze zgodą na dokładną lokalizację, a bez niej ekran prośby odpowiedni do stanu zgody. Po
 * powrocie z ustawień sprawdza zgodę jeszcze raz.
 *
 * Wymagania: NFR-27, BR-8
 *
 * @param onBack „Wstecz” z ekranu prośby
 * @param content ekran, który używa lokalizacji
 */
@Composable
fun WithLocationPermission(onBack: () -> Unit, content: @Composable () -> Unit) {
    LocationPermissionGate(
        permission = { state, onAllow, onOpenSettings ->
            LocationPermissionContent(
                state = state,
                onAllow = onAllow,
                onOpenSettings = onOpenSettings,
                onBack = onBack,
            )
        },
        content = content,
    )
}

/** Wybiera [content] przy dokładnej zgodzie, inaczej przekazuje stan i czynności systemowe do [permission]. */
@Composable
internal fun LocationPermissionGate(
    permission: @Composable (LocationPermissionState, onAllow: () -> Unit, onOpenSettings: () -> Unit) -> Unit,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    var isDenied by rememberSaveable { mutableStateOf(false) }
    var state by rememberSaveable { mutableStateOf(context.permissionNeed(isDenied)) }
    val request =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val isFine = result[Manifest.permission.ACCESS_FINE_LOCATION] == true || context.hasFineLocation()
            val isCoarse = result[Manifest.permission.ACCESS_COARSE_LOCATION] == true || context.hasCoarseLocation()
            isDenied = !isFine && !isCoarse
            state =
                when {
                    isFine -> null
                    isCoarse -> LocationPermissionState.APPROXIMATE
                    else -> LocationPermissionState.DENIED
                }
        }
    LifecycleResumeEffect(Unit) {
        state = context.permissionNeed(isDenied)
        onPauseOrDispose {}
    }
    val current = state
    if (current == null) {
        content()
    } else {
        permission(
            current,
            { request.launch(LOCATION) },
            { context.startActivity(context.appSettings()) },
        )
    }
}

/**
 * Ekran prośby o zgodę na lokalizację dla stanu [state].
 *
 * Wymagania: NFR-27, BR-8, US6.1
 *
 * @param state stan braku dokładnej zgody
 * @param onAllow wywołuje systemowe pytanie o zgodę
 * @param onOpenSettings otwiera ustawienia aplikacji
 * @param onBack „Wstecz”
 * @param modifier wymiary nadane przez aplikację
 */
@Composable
fun LocationPermissionContent(
    state: LocationPermissionState,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MeasureFrame(
        onBack = onBack,
        modifier = modifier,
        actions = {
            PinnedActionBar {
                val action = if (state == LocationPermissionState.DENIED) onOpenSettings else onAllow
                PinnedButton(text = stringResource(state.actionRes), onClick = action)
            }
        },
    ) {
        Text(stringResource(state.messageRes))
    }
}

/**
 * Układ ekranów pomiaru: tło, nagłówek „Wstecz” z tytułem pomiaru, przewijana [content] i przypięte [actions].
 *
 * @param onBack „Wstecz”
 * @param actions pasek akcji przypięty pod treścią
 * @param modifier wymiary nadane przez aplikację
 * @param content treść ekranu
 */
@Composable
internal fun MeasureFrame(
    onBack: () -> Unit,
    actions: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainer) {
        AdaptiveScreen(
            modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
            header = { BackHeader(title = stringResource(R.string.pomiar_naglowek), onBack = onBack) },
            actions = { actions() },
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(Odstepy.m), content = content)
        }
    }
}

private fun Context.appSettings(): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

/** Otwiera ustawienia dostawców lokalizacji systemu. */
internal fun locationSettings(): Intent =
    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
