/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

/**
 * Moduł building: budynki kampusu z konturami i wejściami.
 *
 * <p>Zadanie: lista i karta budynku z wejściami oraz dopasowanie punktu do budynku.</p>
 *
 * <p>API: {@code CampusArea} i {@code Outlines}; operacje HTTP {@code /buildings}.</p>
 *
 * <p>Zależności: {@code shared}.</p>
 *
 * <p>Nie robi: nie przyjmuje ani nie weryfikuje zgłoszeń barier.</p>
 *
 * <p>Kiedy dodawać tutaj: dane budynku, wejść i geometrii kampusu, nie ich odczyt dla zgłoszeń.</p>
 *
 * <p>Wymagania: US1, US2, US17, US3, US16, US15</p>
 *
 * @since 0.1.0
 */
@ApplicationModule(allowedDependencies = "shared")
@NullMarked
package io.github.dostepnykampus.building;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
