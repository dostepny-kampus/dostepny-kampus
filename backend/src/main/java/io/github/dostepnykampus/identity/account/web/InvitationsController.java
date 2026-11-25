/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.identity.account.web;

import io.github.dostepnykampus.identity.account.RoleAlreadyGranted;
import io.github.dostepnykampus.identity.account.StaffRole;
import io.github.dostepnykampus.identity.account.application.InviteAccount;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Zaproszenie Koordynatora: konto z adresem dostaje rolę pracowniczą, a konto, którego nie
 * było, powstaje. Dostęp tylko dla Koordynatora pilnują zabezpieczenia API.
 *
 * <p>Wymagania: US12.1, US12.2, BR-1, BR-2</p>
 *
 * @since 0.1.0
 */
@RestController
public final class InvitationsController {

    /** Przypadek użycia nadania roli. */
    private final InviteAccount invitations;

    InvitationsController(final InviteAccount invitations) {
        this.invitations = invitations;
    }

    /**
     * Nadaje rolę kontu z adresem z zaproszenia; konto, które ma już tę rolę, daje
     * {@link RoleAlreadyGranted}.
     *
     * @param request Adres i rola
     * @return Wynik zaproszenia
     */
    @PostMapping("/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    InvitationResponse createInvitation(@RequestBody final InvitationRequest request) {
        final String email = request.address();
        final StaffRole role = request.staffRole();
        return new InvitationResponse(
            email, role.name(), this.invitations.invite(email, role)
        );
    }
}
