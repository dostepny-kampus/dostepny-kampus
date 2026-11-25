/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.invitation

import io.github.dostepnykampus.core.model.StaffRole
import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.core.network.invitation.Invited
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.IOException

/**
 * Wynik zaproszenia dla ekranu K2.
 *
 * Wymagania: US12.1, US12.2, NFR-50
 */
class ServerInvitationsTest {
    @Test
    fun newAccountIsSent() {
        assertEquals(InviteOutcome.SENT, invite(ApiResult.Success(Invited(isExistingAccount = false))), "nowe konto")
    }

    @Test
    fun existingAccountGetsRole() {
        val outcome = invite(ApiResult.Success(Invited(isExistingAccount = true)))
        assertEquals(InviteOutcome.ROLE_ADDED, outcome, "rola dodana do istniejącego konta")
    }

    @Test
    fun mapsFailures() {
        val expected =
            mapOf(
                ApiResult.ServerError(400) to InviteOutcome.INVALID_EMAIL,
                ApiResult.ServerError(403) to InviteOutcome.FORBIDDEN,
                ApiResult.ServerError(500) to InviteOutcome.FAILED,
                ApiResult.NotFound to InviteOutcome.FAILED,
                ApiResult.NetworkError(IOException("brak sieci")) to InviteOutcome.OFFLINE,
            )
        expected.forEach { (reply, outcome) -> assertEquals(outcome, invite(reply), "odpowiedź $reply") }
    }

    private fun invite(reply: ApiResult<Invited>): InviteOutcome =
        runBlocking { ServerInvitations { _, _ -> reply }.invite("anna.nowak@pwr.edu.pl", StaffRole.VERIFIER) }
}
