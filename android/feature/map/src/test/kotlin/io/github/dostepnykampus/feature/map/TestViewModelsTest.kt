/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TestViewModelsTest {
    private val models = TestViewModels()

    @BeforeEach
    fun main() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun reset() {
        models.resetMain()
    }

    @Test
    fun `zamknięcie czeka na finally`() =
        runTest {
            val viewModel = models.keep(object : ViewModel() {})
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val finished = CompletableDeferred<Unit>()
            viewModel.viewModelScope.launch {
                started.complete(Unit)
                try {
                    awaitCancellation()
                } finally {
                    withContext(NonCancellable) {
                        release.await()
                        finished.complete(Unit)
                    }
                }
            }
            started.await()
            val closing = async { models.cancelAndJoin() }
            try {
                runCurrent()
                assertFalse(closing.isCompleted, "zamknięcie nie czeka na zakończenie pracy ViewModelu")
            } finally {
                release.complete(Unit)
            }
            closing.await()
            assertTrue(finished.isCompleted, "praca ViewModelu została po zamknięciu testu")
        }
}
