/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.identity.web;

import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import tools.jackson.databind.json.JsonMapper;

/**
 * Odpowiedź 429 {@code application/problem+json} z nagłówkiem Retry-After w sekundach.
 *
 * <p>Wymagania: NFR-34</p>
 *
 * @since 0.1.0
 */
final class TooManyRequests {

    /**
     * Zapis ciała odpowiedzi.
     */
    private final JsonMapper mapper;

    TooManyRequests(final JsonMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Zapisuje odmowę.
     *
     * @param response Odpowiedź HTTP
     * @param probe Wynik próby z czasem do odnowienia kubełka
     * @param detail Opis limitu dla klienta
     * @throws IOException Gdy zapis ciała się nie uda
     */
    void send(final HttpServletResponse response, final ConsumptionProbe probe, final String detail)
        throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader(
            HttpHeaders.RETRY_AFTER,
            Long.toString(TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1)
        );
        this.mapper.writeValue(
            response.getOutputStream(),
            Map.of(
                "title", HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                "status", HttpStatus.TOO_MANY_REQUESTS.value(),
                "detail", detail
            )
        );
    }
}
