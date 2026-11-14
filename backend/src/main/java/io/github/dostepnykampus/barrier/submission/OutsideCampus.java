/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission;

import io.github.dostepnykampus.shared.error.InvalidInput;
import java.net.URI;

/**
 * Zgłoszenie spoza terenu PWr: punkt leży dalej niż 500 m od konturu każdego
 * budynku PWr. Odpowiedź 400 ma rodzaj {@link #TYPE}, po
 * którym aplikacja pokazuje stan „Poza terenem PWr”.
 *
 * <p>Wymagania: BR-G5, US4.9</p>
 *
 * @since 0.1.0
 */
public final class OutsideCampus extends InvalidInput {

    /**
     * Rodzaj błędu w polu {@code type} odpowiedzi, jak w kontrakcie API.
     */
    public static final URI TYPE = URI.create("urn:dostepny-kampus:poza-terenem-pwr");

    /**
     * Identyfikator postaci serializowanej wyjątku.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Tworzy błąd zgłoszenia spoza terenu PWr.
     */
    public OutsideCampus() {
        super(
            "Miejsce zgłoszenia leży dalej niż 500 m od każdego budynku PWr (BR-G5)",
            OutsideCampus.TYPE
        );
    }
}
