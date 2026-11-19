/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.content;

import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Miejsce zgłoszenia w punkcie wejścia: punkt, budynek i piętro wejścia.
 *
 * <p>Wymagania: BR-8, BR-14</p>
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface EntrancePlaces {

    /**
     * Daje miejsce zgłoszenia przy wejściu. Wejście wspólne kilku budynków
     * należy do budynku z podpowiedzi, a bez niej do budynku o najmniejszym symbolu.
     *
     * @param id Identyfikator wejścia
     * @param symbol Podpowiedź budynku albo null
     * @return Miejsce w punkcie wejścia albo puste, gdy wejścia nie ma
     */
    Optional<SubmissionPlace> place(String id, @Nullable String symbol);
}
