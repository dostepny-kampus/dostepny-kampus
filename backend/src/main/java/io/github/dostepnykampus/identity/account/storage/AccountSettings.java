/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.identity.account.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Ustawienia konta technicznego serwera w Usłudze logowania {@code dk.accounts} (ADR-0003).
 *
 * <p>Pusty sekret albo adres nie zatrzymuje startu serwera; nadanie roli kończy się wtedy
 * błędem Usługi logowania. Tekst ustawień nie zawiera sekretu.</p>
 *
 * <p>Wymagania: NFR-35</p>
 *
 * @param admin Adres Admin REST API realmu, np. http://localhost:8180/admin/realms/dostepny-kampus
 * @param token Adres wydawania tokenów realmu
 * @param client Identyfikator klienta konta technicznego
 * @param secret Sekret klienta konta technicznego
 * @since 0.1.0
 */
@ConfigurationProperties("dk.accounts")
public record AccountSettings(String admin, String token, String client, String secret) {

    @Override
    public String toString() {
        return String.format(
            "AccountSettings[admin=%s, token=%s, client=%s]", this.admin, this.token, this.client
        );
    }
}
