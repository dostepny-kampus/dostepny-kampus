/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.domain;

import java.security.Principal;
import org.jspecify.annotations.Nullable;

/**
 * Osoba, która czyta zgłoszenie albo jego zdjęcie (BR-5, BR-6).
 *
 * @param account Zalogowane konto albo {@code null} bez logowania; jego nazwa
 *  to identyfikator z tokenu
 * @param verifier Czy konto ma rolę Weryfikatora
 * @since 0.1.0
 */
public record Reader(@Nullable Principal account, boolean verifier) {
}
