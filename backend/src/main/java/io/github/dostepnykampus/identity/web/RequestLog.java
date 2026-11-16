/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.identity.web;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Identyfikator żądania i jedna linia logu na jego koniec. Identyfikator z nagłówka
 * {@code X-Request-Id} (do 64 znaków: litery, cyfry, {@code -} i {@code _}) albo nowy trafia do MDC
 * pod kluczem {@code requestId} i do nagłówka odpowiedzi; po żądaniu znika z MDC. Linia niesie
 * metodę, wzorzec ścieżki, status i czas w milisekundach, bez nagłówków, zapytania i treści:
 * błąd 5xx i żądanie od jednej sekundy to WARN, błąd 4xx to INFO, reszta DEBUG.
 *
 * <p>Wymagania: NFR-57</p>
 *
 * @since 0.1.0
 */
final class RequestLog implements Filter {

    /**
     * Nagłówek z identyfikatorem żądania i odpowiedzi.
     */
    static final String HEADER = "X-Request-Id";

    /**
     * Klucz identyfikatora w MDC.
     */
    static final String KEY = "requestId";

    /**
     * Dozwolona postać identyfikatora przyjętego od klienta.
     */
    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    /**
     * Dziennik żądań.
     */
    private static final Logger LOG = LoggerFactory.getLogger(RequestLog.class);

    @Override
    public void doFilter(
        final ServletRequest request, final ServletResponse response, final FilterChain chain
    ) throws IOException, ServletException {
        final HttpServletRequest http = (HttpServletRequest) request;
        final HttpServletResponse reply = (HttpServletResponse) response;
        final String given = http.getHeader(RequestLog.HEADER);
        String identifier = UUID.randomUUID().toString();
        if (given != null && RequestLog.VALID.matcher(given).matches()) {
            identifier = given;
        }
        MDC.put(RequestLog.KEY, identifier);
        reply.setHeader(RequestLog.HEADER, identifier);
        final long start = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            RequestLog.record(
                http, reply.getStatus(),
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)
            );
            MDC.remove(RequestLog.KEY);
        }
    }

    /**
     * Treść linii logu żądania.
     *
     * @param request Obsłużone żądanie
     * @param status Status odpowiedzi
     * @param millis Czas obsługi w milisekundach
     * @return Metoda, wzorzec ścieżki albo myślnik, status i czas
     */
    static String describe(
        final HttpServletRequest request, final int status, final long millis
    ) {
        String pattern = "-";
        final Object found = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (found instanceof String text) {
            pattern = text;
        }
        return "%s %s %d %d ms".formatted(request.getMethod(), pattern, status, millis);
    }

    private static void record(
        final HttpServletRequest request, final int status, final long millis
    ) {
        final String line = RequestLog.describe(request, status, millis);
        if (status >= 500 || millis >= 1_000L) {
            RequestLog.LOG.warn(line);
        } else if (status >= 400) {
            RequestLog.LOG.info(line);
        } else {
            RequestLog.LOG.debug(line);
        }
    }
}
