/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.identity.account.web;

/**
 * Wynik zaproszenia według schematu Invitation.
 *
 * <p>Wymagania: US12.1, US12.2</p>
 *
 * @param email Adres e-mail zapraszanej osoby
 * @param role Nadana rola
 * @param accountExisted Czy konto istniało i dostało rolę obok dotychczasowych
 * @since 0.1.0
 */
record InvitationResponse(String email, String role, boolean accountExisted) {
}
