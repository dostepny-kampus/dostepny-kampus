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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Operacje publiczne mają limit 60 żądań na minutę z jednego adresu: 61. żądanie dostaje 429
 * {@code application/problem+json} z nagłówkiem Retry-After, a inny adres liczy się osobno.
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
            statuses.add(PublicRateLimitHttpIT.get(context, "198.51.100.7").getStatus());
        }
        final MockHttpServletResponse refused = PublicRateLimitHttpIT.get(
            context, "198.51.100.7"
        );
        MatcherAssert.assertThat(
            "60 żądań nie przeszło albo 61. nie dało 429 z Retry-After problem+json",
            "%b %d %b %s %d".formatted(
                statuses.stream().allMatch(status -> status == 200),
                refused.getStatus(),
                refused.getHeader(HttpHeaders.RETRY_AFTER) != null,
                refused.getContentType(),
                PublicRateLimitHttpIT.get(context, "198.51.100.8").getStatus()
            ),
            Matchers.equalTo("true 429 true application/problem+json 200")
        );
    }

    private static MockHttpServletResponse get(
        final WebApplicationContext context, final String address
    ) throws Exception {
        return context.getBean(MockMvc.class).perform(
            MockMvcRequestBuilders.get("/buildings").with(
                request -> {
                    request.setRemoteAddr(address);
                    return request;
                }
            )
        ).andReturn().getResponse();
    }
}
