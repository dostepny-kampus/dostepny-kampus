/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

/**
 * <p>Zadanie: składa zmiany od kursora z modułów właścicieli danych w jedną odpowiedź.</p>
 *
 * <p>API: brak typów dla innych modułów; HTTP {@code /sync/pull}.</p>
 *
 * <p>Zależności: {@code barrier :: api}, {@code building :: api}, {@code inspection :: api},
 * {@code shared}.</p>
 *
 * <p>Nie robi: nie przechowuje danych i nie zna ich tabel; zmiany daje moduł właściciel.</p>
 *
 * <p>Kiedy dodawać tutaj: składanie pobrania zmian; reguły danych zostają u właściciela.</p>
 *
 * <p>Wymagania: US15.1, US15.2, US8.1, US17.2, BR-20</p>
 *
 * @since 0.1.0
 */
@ApplicationModule(
    allowedDependencies = {"barrier :: api", "building :: api", "inspection :: api", "shared"}
)
@NullMarked
package io.github.dostepnykampus.sync;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
