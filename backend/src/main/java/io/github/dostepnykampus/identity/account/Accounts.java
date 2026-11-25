/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.identity.account;

/**
 * Konta osób w Usłudze logowania.
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface Accounts {

    /**
     * Nadaje rolę kontu z adresem e-mail; bez takiego konta najpierw je zakłada, a nowe konto
     * ma też rolę Odwiedzającego. Dotychczasowe role konta zostają. Gdy Usługa logowania nie
     * odpowiada albo odrzuca zmianę, rzuca {@link IllegalStateException}.
     *
     * <p>Wymagania: US12.1, US12.2, BR-1, BR-2</p>
     *
     * @param email Adres e-mail osoby
     * @param role Nadawana rola pracownicza
     * @return Czy konto powstało, dostało rolę, czy już ją miało
     */
    Grant grant(String email, StaffRole role);
}
