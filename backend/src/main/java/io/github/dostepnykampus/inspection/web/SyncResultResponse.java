/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.inspection.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;

/**
 * Wynik jednej operacji z paczki, schematy AppliedResult i RejectedResult w OpenAPI.
 *
 * @param clientOperationId Identyfikator operacji z żądania
 * @param outcome Rodzaj wyniku: APPLIED albo REJECTED
 * @param problem Powód odrzucenia albo null przy zapisie
 * @since 0.1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record SyncResultResponse(UUID clientOperationId, String outcome, @Nullable Reason problem) {

    /**
     * Wynik operacji zapisanej.
     *
     * @param id Identyfikator operacji z żądania
     * @return Wynik APPLIED
     */
    static SyncResultResponse applied(final UUID id) {
        return new SyncResultResponse(id, "APPLIED", null);
    }

    /**
     * Wynik operacji odrzuconej.
     *
     * @param id Identyfikator operacji z żądania
     * @param status Kod z odpowiedzi HTTP, jaką dałaby operacja wysłana osobno
     * @param detail Opis dla klienta
     * @return Wynik REJECTED z powodem
     */
    static SyncResultResponse rejected(
        final UUID id, final HttpStatus status, final String detail
    ) {
        return new SyncResultResponse(
            id, "REJECTED", new Reason(status.getReasonPhrase(), status.value(), detail)
        );
    }

    /**
     * Powód odrzucenia operacji, schemat Problem w OpenAPI.
     *
     * @param title Krótki opis rodzaju błędu
     * @param status Kod odpowiedzi HTTP
     * @param detail Opis tego wystąpienia błędu
     * @since 0.1.0
     */
    record Reason(String title, int status, String detail) {
    }
}
