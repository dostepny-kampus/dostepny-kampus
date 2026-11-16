/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.identity.web;

import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.Set;
import tools.jackson.databind.json.JsonMapper;

/**
 * Limit zapisów (POST, PUT, PATCH, DELETE) jednego zalogowanego konta w oknie jednej minuty;
 * konto rozpoznaje identyfikator z tokenu. Zapis ponad limit dostaje 429 z nagłówkiem
 * Retry-After i nie idzie dalej. Odczyty i żądania bez logowania przechodzą bez liczenia.
 *
 * <p>Wymagania: NFR-34</p>
 *
 * @since 0.1.0
 */
final class AccountRateLimit implements Filter {

    /**
     * Metody HTTP liczone jako zapis.
     */
    private static final Set<String> WRITES = Set.of("POST", "PUT", "PATCH", "DELETE");

    /**
     * Kubełki według konta.
     */
    private final MinuteBuckets buckets;

    /**
     * Odpowiedź 429.
     */
    private final TooManyRequests refusal;

    AccountRateLimit(final int limit, final JsonMapper mapper) {
        this(new MinuteBuckets(limit), new TooManyRequests(mapper));
    }

    private AccountRateLimit(final MinuteBuckets buckets, final TooManyRequests refusal) {
        this.buckets = buckets;
        this.refusal = refusal;
    }

    @Override
    public void doFilter(
        final ServletRequest request, final ServletResponse response, final FilterChain chain
    ) throws IOException, ServletException {
        final HttpServletRequest http = (HttpServletRequest) request;
        final Principal account = http.getUserPrincipal();
        if (account == null || !AccountRateLimit.WRITES.contains(http.getMethod())) {
            chain.doFilter(request, response);
        } else {
            final ConsumptionProbe probe = this.buckets.take(account.getName());
            if (probe.isConsumed()) {
                chain.doFilter(request, response);
            } else {
                this.refusal.send(
                    (HttpServletResponse) response, probe,
                    "Limit zapisów tego konta na minutę wyczerpany"
                );
            }
        }
    }
}
