/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.ActivityComponent
import io.github.dostepnykampus.core.data.account.RoleProvider
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.help.FirstLaunchViewModel
import io.github.dostepnykampus.feature.map.mapUserAgent

/**
 * Jedyna aktywność aplikacji, rysowana od krawędzi do krawędzi. Po odczycie, czy to pierwsze uruchomienie (US5.1,
 * [FirstLaunchViewModel]), pokazuje oboczkę [AppShell] i zapamiętuje, że instrukcja została zamknięta. Konto i role
 * bierze z grafu Hilt ([MainActivityEntryPoint]).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val firstLaunch: FirstLaunchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        mapUserAgent = "DostepnyKampus/${BuildConfig.VERSION_NAME} (${BuildConfig.APPLICATION_ID})"
        val roles = EntryPointAccessors.fromActivity<MainActivityEntryPoint>(this).roles()
        setContent {
            DostepnyKampusTheme {
                val isFirst by firstLaunch.firstLaunchState.collectAsStateWithLifecycle()
                isFirst?.let { first ->
                    AppShell(firstLaunch = first, roles = roles, onFirstHelpLeft = firstLaunch::markHelpSeen)
                }
            }
        }
    }
}

/**
 * Zależności [MainActivity] z grafu Hilt. Hilt nie wstrzykuje aktywności przez konstruktor, a punkt wejścia daje je
 * bez pól `lateinit`.
 */
@EntryPoint
@InstallIn(ActivityComponent::class)
interface MainActivityEntryPoint {
    /** Konto i role osoby (US14.1). */
    fun roles(): RoleProvider
}
