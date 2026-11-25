/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.entrance;

/**
 * Zmiana dotyczy wejścia usuniętego albo zmienionego po wysłaniu zgłoszenia.
 *
 * <p>Wymagania: BR-23, BR-24</p>
 *
 * @since 0.1.0
 */
public final class StaleEntranceMarking extends RuntimeException {

    /** Identyfikator postaci serializowanej wyjątku. */
    private static final long serialVersionUID = 1L;

    /**
     * Zgłoszenie wymaga ponownej oceny wobec bieżącego oznaczenia.
     */
    public StaleEntranceMarking() {
        super("Zmiana oznaczenia wejścia jest nieaktualna (BR-23)");
    }
}
