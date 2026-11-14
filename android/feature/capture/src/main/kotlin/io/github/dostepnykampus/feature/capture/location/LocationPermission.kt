/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.dostepnykampus.core.designsystem.BackHeader
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.PinnedActionBar
import io.github.dostepnykampus.core.designsystem.PinnedButton
import io.github.dostepnykampus.feature.capture.R
import io.github.dostepnykampus.feature.capture.quick.content

private val LOCATION = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

/** Czy aplikacja ma zgodę na dokładną lokalizację; przybliżona nie wystarczy do pomiaru. Wymagania: US6.1 */
internal fun Context.hasFineLocation(): Boolean =
    checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

/**
 * Pokazuje [content] dopiero ze zgodą na dokładną lokalizację, a bez niej prośbę o zgodę. Po odmowie
 * prowadzi do ustawień aplikacji, a po powrocie z nich sprawdza zgodę jeszcze raz.
 *
 * Wymagania: NFR-27
 *
 * @param onBack „Wstecz” z ekranu prośby
 * @param content ekran, który używa lokalizacji
 */
@Composable
fun WithLocationPermission(onBack: () -> Unit, content: @Composable () -> Unit) {
    val context = LocalContext.current
    var isGranted by rememberSaveable { mutableStateOf(context.hasFineLocation()) }
    var isDenied by rememberSaveable { mutableStateOf(false) }
    val request =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            isGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
            isDenied = !isGranted
        }
    LifecycleResumeEffect(Unit) {
        isGranted = context.hasFineLocation()
        onPauseOrDispose {}
    }
    if (isGranted) {
        content()
    } else {
        LocationPermissionContent(
            isDenied = isDenied,
            onAllow = { request.launch(LOCATION) },
            onOpenSettings = { context.startActivity(context.appSettings()) },
            onBack = onBack,
        )
    }
}

/**
 * Prośba o zgodę na lokalizację: przed pytaniem „Zezwól na lokalizację”, po odmowie „Otwórz ustawienia”.
 *
 * Wymagania: NFR-27
 *
 * @param isDenied czy użytkownik już odmówił
 * @param onAllow systemowe pytanie o zgodę
 * @param onOpenSettings ustawienia aplikacji
 * @param onBack „Wstecz”
 * @param modifier wymiary nadane przez aplikację
 */
@Composable
fun LocationPermissionContent(
    isDenied: Boolean,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            BackHeader(title = stringResource(R.string.pomiar_naglowek), onBack = onBack)
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(Odstepy.m)) {
                Text(stringResource(if (isDenied) R.string.pomiar_zgoda_odmowa else R.string.pomiar_zgoda_tekst))
            }
            PinnedActionBar {
                if (isDenied) {
                    PinnedButton(text = stringResource(R.string.pomiar_otworz_ustawienia), onClick = onOpenSettings)
                } else {
                    PinnedButton(text = stringResource(R.string.pomiar_zgoda_przycisk), onClick = onAllow)
                }
            }
        }
    }
}

private fun Context.appSettings(): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
