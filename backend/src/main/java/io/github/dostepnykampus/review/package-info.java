/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

/**
 * Moduł review: weryfikacja zgłoszeń.
 *
 * <p>Obsługuje kolejkę weryfikacji i decyzję Weryfikatora z powodem
 * odrzucenia (US10). Podgląd podobnych zgłoszeń (US10.1) należy do modułu
 * barrier, bo składa odpowiedź z wewnętrznego zapisu zgłoszenia w JSON.</p>
 *
 * @since 0.1.0
 */
@NullMarked
package io.github.dostepnykampus.review;

import org.jspecify.annotations.NullMarked;
