/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
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
 * <p>{@link InvalidInput} daje 400, {@link MissingResource} 404,
 * {@link MissingLogin} 401, {@link ForbiddenChange} 403,
 * wszystkie z opisem wyjątku w {@code detail}. {@link ConflictingState} daje 409
 * z opisem w {@code detail} i bieżącym stanem jako obiektem JSON we właściwości
 * o nazwie {@link ConflictingState#property()}. Pozostały wyjątek daje 500 bez nazwy
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
     * Dziennik serwera: odrzucone żądania na poziomie DEBUG, nieoczekiwane
     * wyjątki, których klient nie widzi, na poziomie ERROR.
     */
    private final Logger log;

    /**
     * Obsługa z dziennikiem tej klasy.
     */
    public ProblemResponses() {
        this(LoggerFactory.getLogger(ProblemResponses.class));
    }

    ProblemResponses(final Logger log) {
        this.log = log;
    }

    @ExceptionHandler
    ProblemDetail invalid(final InvalidInput error) {
        this.log.debug("Niepoprawne dane żądania: {}", error.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, error.getMessage());
    }

    @ExceptionHandler
    ProblemDetail missing(final MissingResource error) {
        this.log.debug("Brak zasobu: {}", error.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, error.getMessage());
    }

    @ExceptionHandler
    static ProblemDetail unauthorized(final MissingLogin error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, error.getMessage());
    }

    @ExceptionHandler
    static ProblemDetail forbidden(final ForbiddenChange error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, error.getMessage());
    }

    @ExceptionHandler
    ProblemDetail conflict(final ConflictingState error) {
        this.log.debug("Konflikt z bieżącym stanem: {}", error.getMessage());
        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT, error.getMessage()
        );
        problem.setProperty(error.property(), error.value());
        return problem;
    }

    @ExceptionHandler
    ProblemDetail unexpected(final Exception error) {
        this.log.error("Nieobsłużony wyjątek w obsłudze żądania", error);
        return ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
