/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

/**
 * Moduł barrier: zgłoszenia barier i ich zdjęcia.
 *
 * <p>Zadanie: przyjęcie, edycja i status zgłoszenia ze zdjęciami, podgląd podobnych.</p>
 *
 * <p>API: brak typów dla innych modułów; operacje HTTP {@code /submissions}.</p>
 *
 * <p>Zależności: {@code building :: api}, {@code shared}.</p>
 *
 * <p>Nie robi: nie przechowuje budynków i nie podejmuje decyzji Weryfikatora.</p>
 *
 * <p>Kiedy dodawać tutaj: kod zgłoszenia albo zdjęcia; weryfikacja to osobny moduł.</p>
 *
 * <p>Wymagania: US4, US7, US8, US10.1, BR-13</p>
 *
 * @since 0.1.0
 */
@ApplicationModule(allowedDependencies = {"building :: api", "shared"})
@NullMarked
package io.github.dostepnykampus.barrier;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
