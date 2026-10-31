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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.lifecycle.lifecycleScope
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.ActivityComponent
import io.github.dostepnykampus.core.data.account.RoleProvider
import io.github.dostepnykampus.core.data.start.FirstLaunch
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.map.mapUserAgent
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Jedyna aktywność aplikacji, rysowana od krawędzi do krawędzi. Po odczycie, czy to pierwsze uruchomienie (US5.1),
 * pokazuje oboczkę [AppShell] i zapamiętuje, że instrukcja została zamknięta. Zależności bierze z grafu Hilt
 * ([MainActivityEntryPoint]) i podaje oboczce parametrami.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        mapUserAgent = "DostepnyKampus/${BuildConfig.VERSION_NAME} (${BuildConfig.APPLICATION_ID})"
        val graph = EntryPointAccessors.fromActivity<MainActivityEntryPoint>(this)
        val firstLaunch = graph.firstLaunch()
        val roles = graph.roles()
        setContent {
            DostepnyKampusTheme {
                val isFirst by produceState<Boolean?>(initialValue = null) { value = firstLaunch.isFirstLaunch() }
                isFirst?.let { first ->
                    AppShell(
                        firstLaunch = first,
                        roles = roles,
                        onFirstHelpLeft = { markHelpSeen(firstLaunch) },
                    )
                }
            }
        }
    }

    // Zapis kończy się także wtedy, gdy aktywność zniknie w trakcie, np. po drugim „wstecz” zaraz po instrukcji.
    private fun markHelpSeen(firstLaunch: FirstLaunch) {
        lifecycleScope.launch { withContext(NonCancellable) { firstLaunch.markHelpSeen() } }
    }
}

/**
 * Zależności [MainActivity] z grafu Hilt. Hilt nie wstrzykuje aktywności przez konstruktor, a punkt wejścia daje je
 * bez pól `lateinit`.
 */
@EntryPoint
@InstallIn(ActivityComponent::class)
interface MainActivityEntryPoint {
    /** Czy to pierwsze uruchomienie aplikacji (US5.1). */
    fun firstLaunch(): FirstLaunch

    /** Konto i role osoby (US14.1). */
    fun roles(): RoleProvider
}
