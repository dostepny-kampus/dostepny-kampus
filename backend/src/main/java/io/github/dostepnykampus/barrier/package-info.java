/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

/**
 * Moduł barrier: zgłoszenia barier, ich zdjęcia i weryfikacja.
 *
 * <p>Zadanie: przyjęcie, edycja i status zgłoszenia ze zdjęciami, weryfikacja, eksport.</p>
 *
 * <p>API: pakiet {@code api}; HTTP {@code /submissions} i {@code /export.csv}.</p>
 *
 * <p>Zależności: {@code building :: api}, {@code identity :: api}, {@code shared}.</p>
 *
 * <p>Nie robi: nie przechowuje budynków ani kont.</p>
 *
 * <p>Kiedy dodawać tutaj: kod zgłoszenia, jego zdjęcia albo weryfikacji.</p>
 *
 * <p>Wymagania: US4, US7, US8, US10, BR-13</p>
 *
 * @since 0.1.0
 */
@ApplicationModule(allowedDependencies = {"building :: api", "identity :: api", "shared"})
@NullMarked
package io.github.dostepnykampus.barrier;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
