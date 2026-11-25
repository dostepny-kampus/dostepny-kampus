/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.inspection.application;

import io.github.dostepnykampus.identity.api.AccountId;
import io.github.dostepnykampus.inspection.domain.Pseudonyms;
import org.springframework.stereotype.Component;

/**
 * Pseudonimy autorów według reguły modułu tożsamości.
 *
 * <p>Wymagania: BR-5</p>
 *
 * @since 0.1.0
 */
@Component
public final class AccountPseudonyms implements Pseudonyms {

    /**
     * Tworzy pseudonimy według reguły modułu tożsamości.
     */
    public AccountPseudonyms() {
        super();
    }

    @Override
    public String of(final String account) {
        return new AccountId(account).pseudonym();
    }
}
