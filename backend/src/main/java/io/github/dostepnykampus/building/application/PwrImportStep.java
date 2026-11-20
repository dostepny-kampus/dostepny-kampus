/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.application;

import org.springframework.boot.ApplicationArguments;

/**
 * Jeden krok ręcznego importu z usług PWr, np. plany pięter albo warstwa mapy kampusu. Krok jest
 * beanem profilu import-pwr; kolejność kroków wyznacza {@code @Order}. Krok zapisuje swoje dane
 * i nie zamyka aplikacji; robi to {@link PwrImport} po ostatnim kroku.
 *
 * <p>Wymagania: US16, NFR-47</p>
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface PwrImportStep {

    /**
     * Pobiera dane z usługi PWr i zapisuje je. Błąd usługi przerywa cały import.
     *
     * @param args Argumenty polecenia importu, np. --budynek
     */
    void run(ApplicationArguments args);
}
