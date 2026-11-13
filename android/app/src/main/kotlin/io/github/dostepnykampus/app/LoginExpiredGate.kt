/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.data.account.RoleProvider
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.model.isSignedIn
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/**
 * Prośba o ponowne zalogowanie po odpowiedzi 401 przy wysyłce kolejki. Gdy [isExpired], nad
 * aplikacją jest napis „Logowanie wygasło…”, a zgłoszenia czekają w telefonie. Gdy konto z [roles] staje się
 * zalogowane, [onLoggedIn] wznawia wysyłkę bez dotykania ekranu.
 *
 * Wymagania: BR-26, US9.4, US14.4
 *
 * @param isExpired czy kolejka czeka na ponowne zalogowanie
 * @param roles konto osoby; przejście do zalogowanego wznawia wysyłkę
 * @param onLoggedIn wznowienie kolejki po zalogowaniu
 * @param modifier wymiary nadane przez aktywność
 * @param content aplikacja pod napisem
 */
@Composable
fun LoginExpiredGate(
    isExpired: Boolean,
    roles: RoleProvider,
    onLoggedIn: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val loggedIn by rememberUpdatedState(onLoggedIn)
    LaunchedEffect(roles) {
        roles.account
            .map { it.isSignedIn }
            .distinctUntilChanged()
            .drop(1)
            .filter { it }
            .collect { loggedIn() }
    }
    val top = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    Column(modifier = modifier) {
        if (isExpired) {
            Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.logowanie_wygaslo),
                    modifier =
                        Modifier
                            .windowInsetsPadding(top)
                            .padding(Odstepy.m)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
        val below = if (isExpired) Modifier.consumeWindowInsets(top) else Modifier
        Box(modifier = Modifier.weight(1f).then(below)) { content() }
    }
}
