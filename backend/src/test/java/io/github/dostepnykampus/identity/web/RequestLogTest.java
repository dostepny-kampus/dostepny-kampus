/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.identity.web;

import java.util.ArrayList;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Każde żądanie ma identyfikator w MDC i w nagłówku odpowiedzi, a linia logu nie niesie nagłówków
 * ani zapytania.
 *
 * <p>Wymagania: NFR-57</p>
 *
 * @since 0.1.0
 */
final class RequestLogTest {

    @Test
    void echoesValidRequestIdInMdcAndHeader() throws Exception {
        final List<String> seen = new ArrayList<>(1);
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/buildings");
        request.addHeader(RequestLog.HEADER, "klient-7_a");
        final MockHttpServletResponse response = new MockHttpServletResponse();
        new RequestLog().doFilter(
            request, response, (req, res) -> seen.add(MDC.get(RequestLog.KEY))
        );
        MatcherAssert.assertThat(
            "Poprawny identyfikator z żądania nie trafił do MDC i nagłówka odpowiedzi",
            "%s %s".formatted(seen.get(0), response.getHeader(RequestLog.HEADER)),
            Matchers.equalTo("klient-7_a klient-7_a")
        );
    }

    @Test
    void replacesUnsafeRequestIdWithGeneratedOne() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/buildings");
        request.addHeader(RequestLog.HEADER, "zly id\r\n<script>");
        final MockHttpServletResponse response = new MockHttpServletResponse();
        new RequestLog().doFilter(request, response, (req, res) -> { });
        MatcherAssert.assertThat(
            "Niebezpieczny identyfikator z żądania nie został zastąpiony nowym",
            response.getHeader(RequestLog.HEADER),
            Matchers.matchesPattern("[0-9a-f-]{36}")
        );
    }

    @Test
    void createsRequestIdWhenHeaderIsMissing() throws Exception {
        final MockHttpServletResponse response = new MockHttpServletResponse();
        new RequestLog().doFilter(
            new MockHttpServletRequest("GET", "/buildings"), response, (req, res) -> { }
        );
        MatcherAssert.assertThat(
            "Odpowiedź bez identyfikatora w żądaniu nie dostała nowego",
            response.getHeader(RequestLog.HEADER),
            Matchers.matchesPattern("[0-9a-f-]{36}")
        );
    }

    @Test
    void clearsMdcAfterRequest() throws Exception {
        new RequestLog().doFilter(
            new MockHttpServletRequest("GET", "/buildings"),
            new MockHttpServletResponse(),
            (req, res) -> { }
        );
        MatcherAssert.assertThat(
            "Identyfikator żądania został w MDC wątku po zakończeniu żądania",
            MDC.get(RequestLog.KEY),
            Matchers.nullValue()
        );
    }

    @Test
    void describesRequestWithPatternAndWithoutHeadersOrQuery() {
        final MockHttpServletRequest request = new MockHttpServletRequest(
            "GET", "/buildings/C-13"
        );
        request.setQueryString("q=jan.kowalski");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer tajny-token");
        request.setAttribute(
            HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/buildings/{symbol}"
        );
        MatcherAssert.assertThat(
            "Linia logu nie ma wzorca, statusu i czasu albo niesie nagłówek lub zapytanie",
            RequestLog.describe(request, 200, 12L),
            Matchers.equalTo("GET /buildings/{symbol} 200 12 ms")
        );
    }

    @Test
    void describesUnmatchedPathWithoutIt() {
        MatcherAssert.assertThat(
            "Linia logu zawiera ścieżkę, której żaden wzorzec nie dopasował",
            RequestLog.describe(new MockHttpServletRequest("GET", "/wp-login.php"), 404, 1L),
            Matchers.equalTo("GET - 404 1 ms")
        );
    }
}
