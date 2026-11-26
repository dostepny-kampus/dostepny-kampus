/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.inspection.invitation

import io.github.dostepnykampus.core.data.invitation.InviteOutcome
import io.github.dostepnykampus.core.model.StaffRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Stan K2: sprawdzenie adresu i roli, wysłanie zaproszenia i jego wynik.
 *
 * Wymagania: US12.1, US12.2, NFR-50
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InvitationViewModelTest {
    private val sent = mutableListOf<Pair<String, StaffRole>>()

    @Test
    fun refusesBadInput() =
        invitationTest(InviteOutcome.SENT) { viewModel ->
            viewModel.edit("anna.nowak.pwr.edu.pl")
            viewModel.send()
            val expected = setOf(InvitationProblem.EMAIL, InvitationProblem.ROLE)
            assertEquals(expected, viewModel.state.value.problems, "adres bez @ i brak roli")
            assertEquals(emptyList<Pair<String, StaffRole>>(), sent, "nic nie wysłane")
        }

    @Test
    fun sendsTrimmedEmailAndRole() =
        invitationTest(InviteOutcome.SENT) { viewModel ->
            viewModel.edit(" $EMAIL ")
            viewModel.choose(StaffRole.VERIFIER)
            viewModel.send()
            assertEquals(InvitationStage.SENDING, viewModel.state.value.stage, "wysyłanie")
            runCurrent()
            assertEquals(InvitationStage.SENT, viewModel.state.value.stage, "zaproszenie wysłane")
            assertEquals(listOf(EMAIL to StaffRole.VERIFIER), sent, "adres i rola")
            assertEquals(InvitationUiState(stage = InvitationStage.SENT), viewModel.state.value, "pusty formularz")
        }

    @Test
    fun roleAddedKeepsRole() =
        invitationTest(InviteOutcome.ROLE_ADDED) { viewModel ->
            viewModel.edit(EMAIL)
            viewModel.choose(StaffRole.VERIFIER)
            viewModel.send()
            runCurrent()
            val expected = InvitationUiState(stage = InvitationStage.ROLE_ADDED, addedRole = StaffRole.VERIFIER)
            assertEquals(expected, viewModel.state.value, "pusty formularz i dodana rola")
        }

    @Test
    fun mapsOutcomes() {
        val expected =
            mapOf(
                InviteOutcome.ROLE_ADDED to InvitationStage.ROLE_ADDED,
                InviteOutcome.INVALID_EMAIL to InvitationStage.IDLE,
                InviteOutcome.OFFLINE to InvitationStage.OFFLINE,
                InviteOutcome.FORBIDDEN to InvitationStage.FORBIDDEN,
                InviteOutcome.FAILED to InvitationStage.SERVER_ERROR,
            )
        expected.forEach { (outcome, stage) ->
            invitationTest(outcome) { viewModel ->
                viewModel.edit(EMAIL)
                viewModel.choose(StaffRole.COLLECTOR)
                viewModel.send()
                runCurrent()
                assertEquals(stage, viewModel.state.value.stage, "wynik $outcome")
            }
        }
    }

    @Test
    fun serverRefusesEmail() =
        invitationTest(InviteOutcome.INVALID_EMAIL) { viewModel ->
            viewModel.edit(EMAIL)
            viewModel.choose(StaffRole.COLLECTOR)
            viewModel.send()
            runCurrent()
            assertEquals(setOf(InvitationProblem.EMAIL), viewModel.state.value.problems, "adres odrzucony")
        }

    @Test
    fun ignoresSecondTap() =
        invitationTest(InviteOutcome.SENT) { viewModel ->
            viewModel.edit(EMAIL)
            viewModel.choose(StaffRole.COLLECTOR)
            viewModel.send()
            viewModel.send()
            runCurrent()
            assertEquals(1, sent.size, "jedno wysłanie naraz")
        }

    private fun invitationTest(outcome: InviteOutcome, body: suspend TestScope.(InvitationViewModel) -> Unit) {
        val dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        try {
            runTest(dispatcher) {
                body(InvitationViewModel { email, role -> outcome.also { sent += email to role } })
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    private companion object {
        const val EMAIL = "anna.nowak@pwr.edu.pl"
    }
}
