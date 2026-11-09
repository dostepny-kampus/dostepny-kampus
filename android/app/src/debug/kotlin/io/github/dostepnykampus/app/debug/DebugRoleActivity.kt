/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
@file:OptIn(TymczasoweRole::class)

package io.github.dostepnykampus.app.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.github.dostepnykampus.app.R
import io.github.dostepnykampus.core.data.account.TymczasoweRole
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.model.Account

/**
 * Przełącznik ról wersji debug, z osobną ikoną w launcherze: wybór roli zmienia konto w aplikacji i zamyka ekran
 * (US14.1). Wersja release go nie zawiera.
 */
@AndroidEntryPoint
class DebugRoleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val roles = EntryPointAccessors.fromApplication<DebugRolesEntryPoint>(this).roles()
        setContent {
            DostepnyKampusTheme {
                RoleSwitch(
                    current = roles.account,
                    onChoose = { role ->
                        roles.account = role.account
                        finish()
                    },
                )
            }
        }
    }
}

/** [DebugRoles] z grafu Hilt dla [DebugRoleActivity]. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface DebugRolesEntryPoint {
    /** Rola wybrana w przełączniku. */
    fun roles(): DebugRoles
}

/**
 * Cztery role do wyboru; bieżąca ma przycisk wypełniony.
 *
 * @param current bieżące konto
 * @param onChoose wybrana rola
 * @param modifier wymiary nadane przez aktywność
 */
@Composable
fun RoleSwitch(current: Account, onChoose: (DebugRole) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing).padding(Odstepy.m),
            verticalArrangement = Arrangement.spacedBy(Odstepy.s),
        ) {
            Text(stringResource(R.string.debug_rola_tytul), style = MaterialTheme.typography.titleLarge)
            DebugRole.entries.forEach { role ->
                val size = Modifier.fillMaxWidth().heightIn(min = CeleDotyku.przycisk)
                val label = stringResource(role.label)
                if (role.account == current) {
                    Button(onClick = { onChoose(role) }, modifier = size) { Text(label) }
                } else {
                    OutlinedButton(onClick = { onChoose(role) }, modifier = size) { Text(label) }
                }
            }
        }
    }
}
