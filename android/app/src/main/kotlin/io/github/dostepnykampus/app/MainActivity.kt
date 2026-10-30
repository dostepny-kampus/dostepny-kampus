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
import io.github.dostepnykampus.core.data.account.GuestRoleProvider
import io.github.dostepnykampus.core.data.start.FirstLaunch
import io.github.dostepnykampus.core.data.start.firstLaunchRepository
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Jedyna aktywność aplikacji, rysowana od krawędzi do krawędzi. Po odczycie, czy to pierwsze uruchomienie (US5.1),
 * pokazuje oboczkę [AppShell] i zapamiętuje, że instrukcja została zamknięta. Bez logowania w aplikacji każda
 * osoba jest Gościem ([GuestRoleProvider]).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val firstLaunch = firstLaunchRepository()
        setContent {
            DostepnyKampusTheme {
                val isFirst by produceState<Boolean?>(initialValue = null) { value = firstLaunch.isFirstLaunch() }
                isFirst?.let { first ->
                    AppShell(
                        firstLaunch = first,
                        roles = GuestRoleProvider,
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
