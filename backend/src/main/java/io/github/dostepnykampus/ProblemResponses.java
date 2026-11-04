/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Odpowiedzi {@code application/problem+json} (RFC 9457) dla wyjątków
 * rzuconych w obsłudze żądania.
 *
 * <p>{@link InvalidInput} daje 400, {@link MissingResource} 404, oba
 * z opisem wyjątku w {@code detail}. Pozostały wyjątek daje 500 bez nazwy
 * klasy i komunikatu; trafia tylko do dziennika serwera.
 * {@code ResponseStatusException} i {@code ErrorResponseException} obsługuje
 * wcześniej Spring Boot ({@code spring.mvc.problemdetails.enabled}), więc ich
 * odpowiedzi się nie zmieniają.</p>
 *
 * @since 0.1.0
 */
@RestControllerAdvice
public final class ProblemResponses {

    /**
     * Dziennik nieoczekiwanych wyjątków, których klient nie widzi.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ProblemResponses.class);

    @ExceptionHandler
    static ProblemDetail invalid(final InvalidInput error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, error.getMessage());
    }

    @ExceptionHandler
    static ProblemDetail missing(final MissingResource error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, error.getMessage());
    }

    @ExceptionHandler
    static ProblemDetail unexpected(final Exception error) {
        ProblemResponses.LOG.error("Nieobsłużony wyjątek w obsłudze żądania", error);
        return ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
