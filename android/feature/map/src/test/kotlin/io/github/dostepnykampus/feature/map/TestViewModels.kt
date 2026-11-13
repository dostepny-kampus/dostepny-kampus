/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.withContext

/** Kończy korutyny modeli widoku przed zamknięciem ich dyspozytorów i podmiany Main. */
@OptIn(ExperimentalCoroutinesApi::class)
internal class TestViewModels {
    private val jobs = mutableListOf<Job>()

    /** Zapamiętuje korutyny [viewModel] do zakończenia i zwraca ten sam model widoku. */
    fun <T : ViewModel> keep(viewModel: T): T {
        jobs += viewModel.viewModelScope.coroutineContext.job
        return viewModel
    }

    /** Anuluje wszystkie zapamiętane modele i czeka na zakończenie ich pracy, także z bloku `finally`. */
    suspend fun cancelAndJoin() =
        withContext(NonCancellable) {
            jobs.forEach { it.cancel() }
            jobs.forEach { it.join() }
            jobs.clear()
        }

    /** Kończy korutyny przed cofnięciem podmiany Main; także błąd zakończenia przywraca dyspozytor platformy. */
    fun resetMain() =
        runBlocking {
            try {
                cancelAndJoin()
            } finally {
                Dispatchers.resetMain()
            }
        }
}
