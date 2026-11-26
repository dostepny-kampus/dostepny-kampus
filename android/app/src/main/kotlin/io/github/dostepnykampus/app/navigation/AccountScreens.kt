/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import io.github.dostepnykampus.core.model.Account
import io.github.dostepnykampus.core.model.isSignedIn
import io.github.dostepnykampus.feature.auth.AccountActions
import io.github.dostepnykampus.feature.auth.AccountScreen
import io.github.dostepnykampus.feature.auth.SignInActions
import io.github.dostepnykampus.feature.auth.SignInScreen
import io.github.dostepnykampus.feature.auth.SignInViewModel
import io.github.dostepnykampus.feature.auth.WorkAction

/**
 * Ekran konta: L1 „Prośba o logowanie” albo L2 „Konto” zalogowanej osoby. Po wylogowaniu L2 nie rysuje niczego,
 * dopóki nawigator nie otworzy V1.
 *
 * Wymagania: US14.1, US14.2, US14.4
 *
 * @param route L1 albo L2
 * @param signIn stan L1; `null` bierze go z grafu Hilt
 */
@Composable
@NonRestartableComposable
internal fun AppNavigator.Account(route: NavKey, signIn: SignInViewModel? = null) {
    val current by account.collectAsStateWithLifecycle()
    if (route is SignInRoute) {
        val isNowSignedIn = current.isSignedIn
        SideEffect { if (isNowSignedIn) signedIn() }
        val actions = SignInActions(onBack = { back() }, onLeave = { backToMap() })
        SignInScreen(isForReport = route.isForReport, actions = actions, viewModel = signIn ?: hiltViewModel())
        return
    }
    val signedIn = current as? Account.SignedIn ?: return
    val actions = AccountActions(onBack = { back() }, onWork = { work -> openWork(work) }, onSignOut = { signOut() })
    AccountScreen(account = signedIn, actions = actions)
}

private fun AppNavigator.openWork(work: WorkAction) {
    when (work) {
        WorkAction.MEASURE -> measure()
        WorkAction.MINE -> mySubmissions()
        WorkAction.PROGRESS -> inspectionProgress()
        WorkAction.REVIEW -> reviewQueue()
        WorkAction.INVITE -> inspectionInvitation()
        WorkAction.EXPORT -> inspectionExport()
    }
}
