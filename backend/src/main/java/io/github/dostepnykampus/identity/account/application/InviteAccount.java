/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.identity.account.application;

import io.github.dostepnykampus.identity.account.Accounts;
import io.github.dostepnykampus.identity.account.Grant;
import io.github.dostepnykampus.identity.account.RoleAlreadyGranted;
import io.github.dostepnykampus.identity.account.StaffRole;
import org.springframework.stereotype.Service;

/**
 * Nadanie roli pracowniczej kontu przez zaproszenie Koordynatora.
 *
 * <p>Wymagania: US12.1, US12.2, BR-1, BR-2</p>
 *
 * @since 0.1.0
 */
@Service
public final class InviteAccount {

    /** Konta w Usłudze logowania. */
    private final Accounts accounts;

    /**
     * Tworzy przypadek użycia na porcie kont.
     *
     * @param accounts Port kont w Usłudze logowania
     */
    public InviteAccount(final Accounts accounts) {
        this.accounts = accounts;
    }

    /**
     * Nadaje rolę kontu z adresem z zaproszenia; konto z tą samą rolą daje
     * {@link RoleAlreadyGranted}.
     *
     * @param email Adres e-mail zapraszanej osoby
     * @param role Nadawana rola pracownicza
     * @return Prawda, gdy konto istniało już wcześniej
     */
    public boolean invite(final String email, final StaffRole role) {
        final Grant grant = this.accounts.grant(email, role);
        if (grant == Grant.UNCHANGED) {
            throw new RoleAlreadyGranted(role.name());
        }
        return grant == Grant.ADDED;
    }
}
