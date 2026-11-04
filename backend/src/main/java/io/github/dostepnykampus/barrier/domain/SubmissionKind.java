/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.domain;

/**
 * Rodzaj zgłoszenia, wartości jak pole kind schematu SubmissionContent
 * w OpenAPI: pomiar precyzyjny (BR-7), zgłoszenie szybkie (BR-8)
 * i zmiana oznaczenia wejścia (BR-24).
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
     * pozycja (BR-14).
     *
     * @return Prawda tylko dla QUICK
     */
    public boolean buildingFromOutline() {
        return false;
    }
}
