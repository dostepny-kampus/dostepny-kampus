/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.domain;

/**
 * Osoba, która czyta zgłoszenie albo jego zdjęcie (BR-5, BR-6).
 *
 * @param account Identyfikator konta z tokenu; pusty napis bez logowania
 * @param verifier Czy konto ma rolę Weryfikatora
 * @since 0.1.0
 */
public record Reader(String account, boolean verifier) {
}
