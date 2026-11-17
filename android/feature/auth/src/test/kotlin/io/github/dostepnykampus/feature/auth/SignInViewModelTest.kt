/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.auth

import io.github.dostepnykampus.core.data.account.AccountSignIn
import io.github.dostepnykampus.core.data.account.SignInEntry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Stan L1: zwykły, w trakcie logowania, błąd z ponowieniem i brak sieci.
 *
 * Wymagania: US14.2, US14.3, NFR-50
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SignInViewModelTest {
    @Test
    fun returnsToIdleAfterSignIn() =
        viewModelTest { viewModel, service ->
            assertEquals(SignInUiState(), viewModel.state.value, "przed logowaniem")
            viewModel.open(SignInEntry.SIGN_IN)
            runCurrent()
            assertEquals(SignInStage.WORKING, viewModel.state.value.stage, "osoba w przeglądarce")
            service.finish(isSignedIn = true)
            runCurrent()
            assertEquals(SignInStage.IDLE, viewModel.state.value.stage, "po zalogowaniu")
        }

    @Test
    fun retriesLastEntryAfterFailure() =
        viewModelTest { viewModel, service ->
            viewModel.open(SignInEntry.REGISTER)
            runCurrent()
            service.finish(isSignedIn = false)
            runCurrent()
            assertEquals(SignInStage.FAILED, viewModel.state.value.stage, "przerwana rejestracja")
            viewModel.retry()
            runCurrent()
            assertEquals(listOf(SignInEntry.REGISTER, SignInEntry.REGISTER), service.opened, "ta sama strona")
            assertEquals(SignInStage.WORKING, viewModel.state.value.stage, "ponowna próba")
        }

    @Test
    fun ignoresSecondAttemptWhileBusy() =
        viewModelTest { viewModel, service ->
            viewModel.open(SignInEntry.SIGN_IN)
            viewModel.open(SignInEntry.REGISTER)
            runCurrent()
            assertEquals(listOf(SignInEntry.SIGN_IN), service.opened, "jedna próba naraz")
        }

    @Test
    fun followsNetwork() =
        viewModelTest { viewModel, service ->
            service.online.value = false
            runCurrent()
            assertEquals(SignInUiState(isOnline = false), viewModel.state.value, "bez internetu")
            service.online.value = true
            runCurrent()
            assertEquals(SignInUiState(), viewModel.state.value, "internet wrócił")
        }

    private fun viewModelTest(block: suspend TestScope.(SignInViewModel, FakeSignIn) -> Unit) {
        val dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        try {
            runTest(dispatcher) {
                val service = FakeSignIn()
                val viewModel = SignInViewModel(network = { service.online }, signIn = service)
                runCurrent()
                block(viewModel, service)
            }
        } finally {
            Dispatchers.resetMain()
        }
    }
}

/** Usługa logowania, w której próba trwa, aż test ją zakończy przez [finish]. */
private class FakeSignIn : AccountSignIn {
    val online = MutableStateFlow(true)
    val opened = mutableListOf<SignInEntry>()
    private var pending = CompletableDeferred<Boolean>()

    override suspend fun open(entry: SignInEntry): Boolean {
        opened += entry
        pending = CompletableDeferred()
        return pending.await()
    }

    fun finish(isSignedIn: Boolean) {
        pending.complete(isSignedIn)
    }
}
