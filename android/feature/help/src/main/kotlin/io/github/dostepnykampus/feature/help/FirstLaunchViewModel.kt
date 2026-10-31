/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.help

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.dostepnykampus.core.data.start.FirstLaunch
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Czy instrukcja V5 ma się otworzyć sama, bo to pierwsze uruchomienie aplikacji (US5.1). Zapis jest czytany raz,
 * więc obrót ekranu nie czyta go od nowa.
 *
 * @param firstLaunch zapis pierwszego uruchomienia w pliku ustawień startu
 */
@HiltViewModel
class FirstLaunchViewModel
    @Inject
    constructor(private val firstLaunch: FirstLaunch) : ViewModel() {
        /** Czy to pierwsze uruchomienie; `null`, dopóki zapis nie jest odczytany. */
        val firstLaunchState: StateFlow<Boolean?> =
            flow { emit(firstLaunch.isFirstLaunch()) }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

        /**
         * Zapisuje, że instrukcja z pierwszego uruchomienia została zamknięta. Zapis kończy się także wtedy, gdy
         * aktywność zniknie w trakcie, np. po drugim „wstecz” zaraz po instrukcji.
         */
        fun markHelpSeen() {
            viewModelScope.launch { withContext(NonCancellable) { firstLaunch.markHelpSeen() } }
        }
    }
