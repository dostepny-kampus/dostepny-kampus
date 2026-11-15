/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.identity.web;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

/**
 * Limit żądań bez nagłówka Authorization z jednego adresu klienta w oknie jednej minuty
 * (Bucket4j, kubełek tokenów odnawiany co minutę). Żądanie ponad limit dostaje 429
 * {@code application/problem+json} z nagłówkiem Retry-After w sekundach i nie idzie dalej.
 * Kubełki adresów nieaktywnych dłużej niż dwie minuty znikają z pamięci.
 *
 * <p>Wymagania: NFR-34</p>
 *
 * @since 0.1.0
 */
final class PublicRateLimit extends OncePerRequestFilter {

    /**
     * Ustawienia limitu.
     */
    private final RateLimitSettings settings;

    /**
     * Kubełki według adresu klienta.
     */
    private final Cache<String, Bucket> buckets;

    /**
     * Zapis odpowiedzi 429.
     */
    private final JsonMapper mapper;

    PublicRateLimit(final RateLimitSettings settings, final JsonMapper mapper) {
        super();
        this.settings = settings;
        this.mapper = mapper;
        this.buckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(2))
            .maximumSize(100_000)
            .build();
    }

    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) {
        return request.getHeader(HttpHeaders.AUTHORIZATION) != null
            || this.settings.unlimitedAddresses().contains(request.getRemoteAddr());
    }

    @Override
    protected void doFilterInternal(
        final HttpServletRequest request, final HttpServletResponse response,
        final FilterChain chain
    ) throws ServletException, IOException {
        final ConsumptionProbe probe = this.buckets
            .get(request.getRemoteAddr(), address -> this.bucket())
            .tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setHeader(
                HttpHeaders.RETRY_AFTER,
                Long.toString(
                    Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1)
                )
            );
            this.mapper.writeValue(
                response.getOutputStream(),
                Map.of(
                    "title", HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                    "status", HttpStatus.TOO_MANY_REQUESTS.value(),
                    "detail", "Limit żądań bez logowania z tego adresu na minutę wyczerpany"
                )
            );
        }
    }

    private Bucket bucket() {
        final int limit = this.settings.publicPerMinute();
        return Bucket.builder()
            .addLimit(
                Bandwidth.builder().capacity(limit)
                    .refillIntervally(limit, Duration.ofMinutes(1)).build()
            )
            .build();
    }
}
