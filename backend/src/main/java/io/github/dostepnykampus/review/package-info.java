/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

/**
 * Moduł review: weryfikacja zgłoszeń.
 *
 * <p>Obsługuje kolejkę weryfikacji i decyzję Weryfikatora z powodem
 * odrzucenia. Podgląd podobnych zgłoszeń należy do modułu
 * barrier, bo składa odpowiedź z wewnętrznego zapisu zgłoszenia w JSON.</p>
 *
 * <p>Wymagania: US10, US10.1</p>
 *
 * @since 0.1.0
 */
@ApplicationModule(allowedDependencies = {})
@NullMarked
package io.github.dostepnykampus.review;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
