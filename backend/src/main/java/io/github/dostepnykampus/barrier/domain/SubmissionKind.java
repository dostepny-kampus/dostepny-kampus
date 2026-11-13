/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.domain;

/**
 * Rodzaj zgłoszenia, wartości jak pole kind schematu SubmissionContent
 * w OpenAPI: pomiar precyzyjny, zgłoszenie szybkie
 * i zmiana oznaczenia wejścia.
 *
 * <p>Wymagania: BR-7, BR-8, BR-24</p>
 *
 * @since 0.1.0
 */
public enum SubmissionKind {

    /** Pomiar precyzyjny. */
    PRECISE,

    /** Zgłoszenie szybkie. */
    QUICK,

    /** Zmiana oznaczenia wejścia. */
    ENTRANCE_MARKING;

    /**
     * Czy budynek zgłoszenia przypisuje system z konturu, w którym leży
     * pozycja.
     *
     * <p>Wymagania: BR-14</p>
     *
     * @return Prawda tylko dla QUICK
     */
    public boolean buildingFromOutline() {
        return this == SubmissionKind.QUICK;
    }
}
