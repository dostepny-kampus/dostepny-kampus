/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.inspection.invitation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.dostepnykampus.core.data.invitation.InviteOutcome
import io.github.dostepnykampus.core.data.invitation.Invitations
import io.github.dostepnykampus.core.model.StaffRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Etap wysłania zaproszenia na K2. Wymagania: US12.1, US12.2, NFR-50 */
enum class InvitationStage {
    /** Adres i rola przed wysłaniem. */
    IDLE,

    /** Serwer przyjmuje zaproszenie; drugie wysłanie nie startuje. */
    SENDING,

    /** Zaproszenie wysłane na nowy adres. */
    SENT,

    /** Konto istniało i dostało rolę obok dotychczasowych. */
    ROLE_ADDED,

    /** Bez internetu. */
    OFFLINE,

    /** Błąd serwera albo Usługi logowania. */
    SERVER_ERROR,

    /** Konto nie ma roli Koordynatora. */
    FORBIDDEN,
}

/** Błąd danych zaproszenia, przez który się nie wysyła. Wymagania: US12.1, PW-10 */
enum class InvitationProblem {
    /** Adres e-mail niepełny. */
    EMAIL,

    /** Rola niewybrana. */
    ROLE,
}

/**
 * Stan ekranu K2.
 *
 * @property email wpisany adres e-mail
 * @property role wybrana rola; `null`, dopóki osoba jej nie wybierze
 * @property stage etap wysłania
 * @property problems błędy danych z ostatniej próby wysłania
 * @property addedRole rola dodana istniejącemu kontu przy [InvitationStage.ROLE_ADDED]; `null` w innych etapach
 */
data class InvitationUiState(
    val email: String = "",
    val role: StaffRole? = null,
    val stage: InvitationStage = InvitationStage.IDLE,
    val problems: Set<InvitationProblem> = emptySet(),
    val addedRole: StaffRole? = null,
)

/**
 * K2 „Zaproszenie”: adres e-mail i rola, sprawdzenie ich przed wysłaniem i wynik zaproszenia. Po wysłaniu
 * formularz jest pusty, więc następne zaproszenie zaczyna się od nowa.
 *
 * Wymagania: US12.1, US12.2, NFR-50, PW-10
 *
 * @param invitations wysłanie zaproszenia
 */
@HiltViewModel
class InvitationViewModel
    @Inject
    constructor(private val invitations: Invitations) : ViewModel() {
        private val current = MutableStateFlow(InvitationUiState())

        /** Stan ekranu. */
        val state: StateFlow<InvitationUiState> = current.asStateFlow()

        /** Zmienia wpisany adres na [email]. */
        fun edit(email: String) {
            current.update { it.copy(email = email) }
        }

        /** Wybiera rolę [role]. */
        fun choose(role: StaffRole) {
            current.update { it.copy(role = role, problems = it.problems - InvitationProblem.ROLE) }
        }

        /** „Wyślij zaproszenie”: przy błędnych danych tylko je wskazuje; w trakcie wysyłania nic nie robi. */
        fun send() {
            val now = current.value
            if (now.stage == InvitationStage.SENDING) return
            val email = now.email.trim()
            val problems = problemsOf(email, now.role)
            val role = now.role
            if (problems.isNotEmpty() || role == null) {
                current.update { it.copy(stage = InvitationStage.IDLE, problems = problems) }
                return
            }
            current.update { it.copy(stage = InvitationStage.SENDING, problems = emptySet(), addedRole = null) }
            viewModelScope.launch { finish(invitations.invite(email, role), role) }
        }

        private fun finish(outcome: InviteOutcome, role: StaffRole) {
            val stage = stageOf(outcome)
            if (stage == InvitationStage.SENT || stage == InvitationStage.ROLE_ADDED) {
                val added = role.takeIf { stage == InvitationStage.ROLE_ADDED }
                current.value = InvitationUiState(stage = stage, addedRole = added)
            } else {
                val problems =
                    if (outcome == InviteOutcome.INVALID_EMAIL) setOf(InvitationProblem.EMAIL) else emptySet()
                current.update { it.copy(stage = stage, problems = problems) }
            }
        }
    }

private val EMAIL_SHAPE = Regex("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")

private fun problemsOf(email: String, role: StaffRole?): Set<InvitationProblem> =
    buildSet {
        if (!EMAIL_SHAPE.matches(email)) add(InvitationProblem.EMAIL)
        if (role == null) add(InvitationProblem.ROLE)
    }

private fun stageOf(outcome: InviteOutcome): InvitationStage =
    when (outcome) {
        InviteOutcome.SENT -> InvitationStage.SENT
        InviteOutcome.ROLE_ADDED -> InvitationStage.ROLE_ADDED
        InviteOutcome.INVALID_EMAIL -> InvitationStage.IDLE
        InviteOutcome.OFFLINE -> InvitationStage.OFFLINE
        InviteOutcome.FORBIDDEN -> InvitationStage.FORBIDDEN
        InviteOutcome.FAILED -> InvitationStage.SERVER_ERROR
    }
