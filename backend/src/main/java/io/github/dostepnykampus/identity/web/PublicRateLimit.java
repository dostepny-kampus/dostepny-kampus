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
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import tools.jackson.databind.json.JsonMapper;

/**
 * Limit żądań bez nagłówka Authorization z jednego adresu klienta w oknie jednej minuty
 * (Bucket4j, kubełek tokenów odnawiany co minutę). Żądanie ponad limit dostaje 429
 * {@code application/problem+json} z nagłówkiem Retry-After w sekundach i nie idzie dalej.
 * Kubełki adresów nieaktywnych dłużej niż dwie minuty znikają z pamięci (Caffeine).
 *
 * <p>Wymagania: NFR-34</p>
 *
 * @since 0.1.0
 */
final class PublicRateLimit implements Filter {

    /**
     * Ustawienia limitu.
     */
    private final RateLimitSettings settings;

    /**
     * Zapis odpowiedzi 429.
     */
    private final JsonMapper mapper;

    /**
     * Kubełki według adresu klienta.
     */
    private final Cache<String, Bucket> buckets;

    PublicRateLimit(final RateLimitSettings settings, final JsonMapper mapper) {
        this(
            settings, mapper,
            Caffeine.newBuilder().expireAfterAccess(Duration.ofMinutes(2))
                .maximumSize(100_000).build()
        );
    }

    private PublicRateLimit(
        final RateLimitSettings settings, final JsonMapper mapper,
        final Cache<String, Bucket> buckets
    ) {
        this.settings = settings;
        this.mapper = mapper;
        this.buckets = buckets;
    }

    @Override
    public void doFilter(
        final ServletRequest request, final ServletResponse response, final FilterChain chain
    ) throws IOException, ServletException {
        final HttpServletRequest http = (HttpServletRequest) request;
        if (http.getHeader(HttpHeaders.AUTHORIZATION) != null
            || this.settings.unlimitedAddresses().contains(http.getRemoteAddr())) {
            chain.doFilter(request, response);
        } else {
            final ConsumptionProbe probe = this.buckets
                .get(http.getRemoteAddr(), address -> this.bucket())
                .tryConsumeAndReturnRemaining(1);
            if (probe.isConsumed()) {
                chain.doFilter(request, response);
            } else {
                this.refuse((HttpServletResponse) response, probe);
            }
        }
    }

    private void refuse(final HttpServletResponse response, final ConsumptionProbe probe)
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
                "detail", "Limit żądań bez logowania z tego adresu na minutę wyczerpany"
            )
        );
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
