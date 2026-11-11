/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
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
import androidx.lifecycle.lifecycleScope
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.ActivityComponent
import io.github.dostepnykampus.core.data.account.RoleProvider
import io.github.dostepnykampus.core.data.map.MapListChoice
import io.github.dostepnykampus.core.data.submissions.SubmissionQueue
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.help.FirstLaunchViewModel
import kotlinx.coroutines.launch

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
        val graph = EntryPointAccessors.fromActivity<MainActivityEntryPoint>(this)
        val onChooseMap: () -> Unit = { lifecycleScope.launch { graph.mapListChoice().choose(isList = false) } }
        setContent {
            DostepnyKampusTheme {
                val isFirst by firstLaunch.firstLaunchState.collectAsStateWithLifecycle()
                val queue = graph.submissionQueue()
                val isLoginExpired by queue.loginExpired.collectAsStateWithLifecycle()
                LoginExpiredGate(isExpired = isLoginExpired, roles = graph.roles(), onLoggedIn = queue::onLoggedIn) {
                    isFirst?.let { first ->
                        AppShell(
                            firstLaunch = first,
                            roles = graph.roles(),
                            onFirstHelpLeft = firstLaunch::markHelpSeen,
                            onChooseMap = onChooseMap,
                        )
                    }
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

    /** Wybór „Mapa” albo „Lista budynków” w V1 (NFR-19, decyzja 116). */
    fun mapListChoice(): MapListChoice

    /** Kolejka zgłoszeń: prośba o logowanie po 401 i wznowienie po zalogowaniu (BR-26). */
    fun submissionQueue(): SubmissionQueue
}
