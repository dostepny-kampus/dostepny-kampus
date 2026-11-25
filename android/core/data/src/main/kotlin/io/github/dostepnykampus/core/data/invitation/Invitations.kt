/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.invitation

import io.github.dostepnykampus.core.model.StaffRole
import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.core.network.invitation.Invited
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.net.HttpURLConnection.HTTP_FORBIDDEN

/**
 * Wynik zaproszenia dla ekranu K2.
 *
 * TODO(#3826): stan „ma już rolę” z odpowiedzi 409 według kontraktu po zmianie.
 *
 * Wymagania: US12.1, US12.2, NFR-50
 */
public enum class InviteOutcome {
    /** Zaproszenie wysłane na nowy adres. */
    SENT,

    /** Konto z tym adresem istniało i dostało rolę obok dotychczasowych. */
    ROLE_ADDED,

    /** Serwer odrzucił adres e-mail (400). */
    INVALID_EMAIL,

    /** Brak połączenia z serwerem. */
    OFFLINE,

    /** Konto nie ma roli Koordynatora (403). */
    FORBIDDEN,

    /** Błąd serwera albo Usługi logowania. */
    FAILED,
}

/** Zaproszenie osoby z rolą przez Koordynatora; wymaga sieci. Wymagania: US12.1, US12.2 */
public fun interface Invitations {
    /** Zaprasza osobę z adresem [email] i rolą [role]. */
    public suspend fun invite(email: String, role: StaffRole): InviteOutcome
}

/** Zaproszenia wysyłane przez [send] do serwera. */
internal class ServerInvitations(private val send: suspend (String, StaffRole) -> ApiResult<Invited>) : Invitations {
    override suspend fun invite(email: String, role: StaffRole): InviteOutcome =
        when (val reply = send(email, role)) {
            is ApiResult.Success -> if (reply.value.isExistingAccount) InviteOutcome.ROLE_ADDED else InviteOutcome.SENT
            is ApiResult.ServerError -> refused(reply.code)
            is ApiResult.NetworkError -> InviteOutcome.OFFLINE
            ApiResult.NotFound -> InviteOutcome.FAILED
        }

    private fun refused(code: Int): InviteOutcome =
        when (code) {
            HTTP_BAD_REQUEST -> InviteOutcome.INVALID_EMAIL
            HTTP_FORBIDDEN -> InviteOutcome.FORBIDDEN
            else -> InviteOutcome.FAILED
        }
}
