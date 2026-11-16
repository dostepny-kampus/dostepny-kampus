/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import jakarta.servlet.ServletInputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.DelegatingServletInputStream;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

/**
 * HTTP odrzuca ciało JSON ponad 1 000 000 bajtów odpowiedzią 413, także bez nagłówka
 * Content-Length, i nie czyta strumienia dalej niż o bajt za limit.
 *
 * <p>Wymagania: NFR-34</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
final class JsonBodyLimitHttpIT {

    /**
     * Rozmiar ciała ponad limitem.
     */
    private static final int OVERSIZED = 1_500_000;

    @ParameterizedTest
    @ValueSource(longs = {-1, JsonBodyLimitHttpIT.OVERSIZED})
    void rejectsOversizedBody(final long declared, final WebApplicationContext context)
        throws Exception {
        final ByteArrayInputStream source = JsonBodyLimitHttpIT.body(JsonBodyLimitHttpIT.OVERSIZED);
        final MockHttpServletResponse response = JsonBodyLimitHttpIT.post(
            context, source, declared
        );
        MatcherAssert.assertThat(
            "Ciało JSON ponad limit nie daje 413 problem+json albo jest czytane do końca",
            List.of(
                response.getStatus(),
                MediaType.parseMediaType(response.getContentType()).getSubtype(),
                JsonBodyLimitHttpIT.OVERSIZED - source.available() <= 1_000_001
            ),
            Matchers.equalTo(List.of(413, "problem+json", true))
        );
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 999_000})
    void readsBodyWithinLimit(final long declared, final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Ciało JSON w limicie dało 413",
            JsonBodyLimitHttpIT.post(
                context, JsonBodyLimitHttpIT.body(999_000), declared
            ).getStatus(),
            Matchers.equalTo(400)
        );
    }

    private static ByteArrayInputStream body(final int size) {
        final String head = String.join(
            "", "{\"id\":\"", UUID.randomUUID().toString(), "\",\"kind\":\"QUICK\",\"description\":\""
        );
        return new ByteArrayInputStream(
            String.join("", head, "a".repeat(size - head.length() - 2), "\"}")
                .getBytes(StandardCharsets.UTF_8)
        );
    }

    private static MockHttpServletResponse post(
        final WebApplicationContext context, final ByteArrayInputStream source,
        final long declared
    ) throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest(
            context.getServletContext()
        ) {
            @Override
            public long getContentLengthLong() {
                return declared;
            }

            @Override
            public ServletInputStream getInputStream() {
                return new DelegatingServletInputStream(source);
            }
        };
        request.setMethod("POST");
        request.setRequestURI("/submissions");
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        new AsCollector().postProcessRequest(request);
        if (declared >= 0) {
            request.addHeader(HttpHeaders.CONTENT_LENGTH, declared);
        }
        return context.getBean(MockMvc.class).perform(servlet -> request)
            .andReturn().getResponse();
    }
}
