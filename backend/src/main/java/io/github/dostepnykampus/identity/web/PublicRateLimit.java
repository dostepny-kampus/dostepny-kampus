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
import tools.jackson.databind.json.JsonMapper;

/**
 * Limit żądań bez logowania z jednego adresu klienta w oknie jednej minuty.
 * Żądanie ponad limit dostaje 429 z nagłówkiem Retry-After i nie idzie dalej.
 *
 * <p>Wymagania: NFR-34</p>
 *
 * @since 0.1.0
 */
final class PublicRateLimit implements Filter {

    /**
     * Ustawienia limitu z adresami bez limitu.
     */
    private final RateLimitSettings settings;

    /**
     * Kubełki według adresu klienta.
     */
    private final MinuteBuckets buckets;

    /**
     * Odpowiedź 429.
     */
    private final TooManyRequests refusal;

    PublicRateLimit(final RateLimitSettings settings, final JsonMapper mapper) {
        this(settings, new MinuteBuckets(settings.publicPerMinute()), new TooManyRequests(mapper));
    }

    private PublicRateLimit(
        final RateLimitSettings settings, final MinuteBuckets buckets,
        final TooManyRequests refusal
    ) {
        this.settings = settings;
        this.buckets = buckets;
        this.refusal = refusal;
    }

    @Override
    public void doFilter(
        final ServletRequest request, final ServletResponse response, final FilterChain chain
    ) throws IOException, ServletException {
        final HttpServletRequest http = (HttpServletRequest) request;
        if (http.getUserPrincipal() != null
            || this.settings.exempt().contains(http.getRemoteAddr())) {
            chain.doFilter(request, response);
        } else {
            final ConsumptionProbe probe = this.buckets.take(http.getRemoteAddr());
            if (probe.isConsumed()) {
                chain.doFilter(request, response);
            } else {
                this.refusal.send(
                    (HttpServletResponse) response, probe,
                    "Limit żądań bez logowania z tego adresu na minutę wyczerpany"
                );
            }
        }
    }
}
