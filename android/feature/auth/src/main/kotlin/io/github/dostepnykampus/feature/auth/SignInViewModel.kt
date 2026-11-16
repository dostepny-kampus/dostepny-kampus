/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.dostepnykampus.core.data.account.AccountSignIn
import io.github.dostepnykampus.core.data.account.SignInEntry
import io.github.dostepnykampus.core.data.network.NetworkStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Etap logowania na L1. Wymagania: US14.2, NFR-50 */
enum class SignInStage {
    /** Zdanie o koncie i trzy przyciski. */
    IDLE,

    /** Osoba jest na stronie Usługi logowania; druga próba nie startuje. */
    WORKING,

    /** Logowanie się nie udało albo zostało przerwane. */
    FAILED,
}

/**
 * Stan ekranu L1.
 *
 * @property isOnline czy jest internet; bez niego logowania nie da się zacząć
 * @property stage etap logowania
 */
data class SignInUiState(val isOnline: Boolean = true, val stage: SignInStage = SignInStage.IDLE)

/**
 * L1 „Prośba o logowanie”: „Zaloguj się” i „Załóż konto” otwierają Usługę logowania, a po błędzie
 * „Spróbuj ponownie” powtarza ostatnią próbę. Po zalogowaniu konto zmienia się w sesji konta.
 *
 * Wymagania: US14.2, US14.3, NFR-50
 *
 * @param network stan sieci
 * @param signIn logowanie w Usłudze logowania
 */
@HiltViewModel
class SignInViewModel
    @Inject
    constructor(network: NetworkStatus, private val signIn: AccountSignIn) :
    ViewModel() {
        private val stage = MutableStateFlow(SignInStage.IDLE)
        private var lastEntry = SignInEntry.SIGN_IN

        /** Stan ekranu. */
        val state: StateFlow<SignInUiState> =
            combine(network.isOnline(), stage) { isOnline, current -> SignInUiState(isOnline, current) }
                .stateIn(viewModelScope, SharingStarted.Eagerly, SignInUiState())

        /** „Zaloguj się” albo „Załóż konto”; w trakcie próby kolejne dotknięcie nic nie robi. */
        fun open(entry: SignInEntry) {
            if (stage.value == SignInStage.WORKING) return
            lastEntry = entry
            stage.value = SignInStage.WORKING
            viewModelScope.launch {
                stage.value = if (signIn.open(entry)) SignInStage.IDLE else SignInStage.FAILED
            }
        }

        /** „Spróbuj ponownie” po błędzie: ta sama strona co w ostatniej próbie. */
        fun retry() {
            open(lastEntry)
        }
    }
