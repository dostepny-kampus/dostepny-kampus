/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

/**
 * Testy modułu pobrania zmian: zmiany od kursora z modułów właścicieli danych.
 *
 * <p>Wymagania: US15.1, US15.2, US8.1, BR-20</p>
 *
 * @since 0.1.0
 */
@ApplicationModule(
    allowedDependencies = {"barrier :: api", "building :: api", "inspection :: api", "shared"}
)
package io.github.dostepnykampus.sync;

import org.springframework.modulith.ApplicationModule;
