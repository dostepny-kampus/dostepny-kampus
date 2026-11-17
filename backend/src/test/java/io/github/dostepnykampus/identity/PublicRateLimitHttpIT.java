/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.identity;

import io.github.dostepnykampus.IntegrationTest;
import java.util.ArrayList;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Operacje publiczne mają limit 60 żądań na minutę z jednego adresu: 61. żądanie dostaje 429
 * {@code application/problem+json} z nagłówkiem Retry-After, także z obcym nagłówkiem
 * Authorization, a inny adres, ważne logowanie i adres z listy wyjątków liczą się osobno
 * albo wcale.
 *
 * <p>Wymagania: NFR-34</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
final class PublicRateLimitHttpIT {

    @Test
    void refusesSixtyFirstRequestInMinute(final WebApplicationContext context)
        throws Exception {
        final List<Integer> statuses = new ArrayList<>(60);
        for (int idx = 0; idx < 60; idx += 1) {
            statuses.add(PublicRateLimitHttpIT.get(context, "198.51.100.7", "").getStatus());
        }
        final MockHttpServletResponse refused = PublicRateLimitHttpIT.get(
            context, "198.51.100.7", ""
        );
        MatcherAssert.assertThat(
            "60 żądań nie przeszło albo 61. nie dało 429 z Retry-After problem+json",
            "%b %d %b %s %d".formatted(
                statuses.stream().allMatch(status -> status == 200),
                refused.getStatus(),
                refused.getHeader(HttpHeaders.RETRY_AFTER) != null,
                refused.getContentType(),
                PublicRateLimitHttpIT.get(context, "198.51.100.8", "").getStatus()
            ),
            Matchers.equalTo("true 429 true application/problem+json 200")
        );
    }

    @Test
    void countsForeignAuthorizationHeaderAndSkipsValidTokenOrExemptAddress(
        final WebApplicationContext context
    ) throws Exception {
        final List<Integer> disguised = new ArrayList<>(60);
        for (int idx = 0; idx < 60; idx += 1) {
            disguised.add(
                PublicRateLimitHttpIT.get(context, "198.51.100.9", "Basic eDp4").getStatus()
            );
        }
        final MockHttpServletResponse refused = PublicRateLimitHttpIT.get(
            context, "198.51.100.9", "Basic eDp4"
        );
        final List<Integer> exempt = new ArrayList<>(61);
        for (int idx = 0; idx < 61; idx += 1) {
            exempt.add(
                PublicRateLimitHttpIT.get(context, "127.0.0.1", "Basic eDp4").getStatus()
            );
        }
        MatcherAssert.assertThat(
            "Obcy nagłówek ominął limit, ważny token zużył limit albo wyjątek dostał 429",
            "%b %d %b %s %d %b".formatted(
                disguised.stream().allMatch(status -> status == 200),
                refused.getStatus(),
                refused.getHeader(HttpHeaders.RETRY_AFTER) != null,
                refused.getContentType(),
                PublicRateLimitHttpIT.get(context, "198.51.100.9", "Bearer VISITOR")
                    .getStatus(),
                exempt.stream().allMatch(status -> status == 200)
            ),
            Matchers.equalTo("true 429 true application/problem+json 200 true")
        );
    }

    private static MockHttpServletResponse get(
        final WebApplicationContext context, final String address, final String authorization
    ) throws Exception {
        final MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/buildings")
            .with(
                servlet -> {
                    servlet.setRemoteAddr(address);
                    return servlet;
                }
            );
        if (!authorization.isEmpty()) {
            request.header(HttpHeaders.AUTHORIZATION, authorization);
        }
        return context.getBean(MockMvc.class).perform(request).andReturn().getResponse();
    }
}
