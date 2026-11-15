/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import io.github.dostepnykampus.app.R
import io.github.dostepnykampus.core.designsystem.bar.BackHeader
import io.github.dostepnykampus.core.model.Account
import io.github.dostepnykampus.core.model.StaffRole
import io.github.dostepnykampus.core.model.hasStaffRole
import io.github.dostepnykampus.core.model.isSignedIn
import io.github.dostepnykampus.feature.capture.R as CaptureR
import io.github.dostepnykampus.feature.inspection.R as InspectionR

private typealias Action = Pair<String, () -> Unit>

private val Account.isCoordinator: Boolean
    get() = this is Account.SignedIn && StaffRole.COORDINATOR in staffRoles

/**
 * Ekran konta: logowanie albo L2 „Konto i praca” jako zaślepka z czynnościami roli. „Postęp inwentaryzacji”
 * widzi tylko Koordynator. Zaślepki nie zapamiętują stanu, więc nie muszą się odświeżać osobno od nawigatora.
 *
 * Wymagania: US14.2, US11.1
 */
@Composable
@NonRestartableComposable
internal fun AppNavigator.Account(route: NavKey) {
    val current by account.collectAsStateWithLifecycle()
    val isWork = current.hasStaffRole && route == AccountRoute
    val title = stringResource(if (isWork) R.string.nav_konto_i_praca else R.string.nav_konto)
    when {
        route is SignInRoute -> {
            val isNowSignedIn = current.isSignedIn
            SideEffect { if (isNowSignedIn) signedIn() }
            Stub(title, stringResource(R.string.nie_teraz) to { backToMap() })
        }

        isWork -> {
            val progress = stringResource(InspectionR.string.postep_naglowek) to { inspectionProgress() }
            val toMeasure = stringResource(R.string.zmierz_i_zglos) to { measure() }
            val toMine = stringResource(CaptureR.string.moje_naglowek) to { mySubmissions() }
            if (current.isCoordinator) Stub(title, toMeasure, toMine, progress) else Stub(title, toMeasure, toMine)
        }

        else -> {
            Stub(title)
        }
    }
}

@Composable
@NonRestartableComposable
private fun AppNavigator.Stub(title: String, vararg actions: Action) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            BackHeader(title = title, onBack = { back() })
            actions.forEach { (label, onClick) -> TextButton(onClick = onClick) { Text(text = label) } }
        }
    }
}
