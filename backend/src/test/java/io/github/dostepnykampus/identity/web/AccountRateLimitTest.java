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
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

/**
 * Zapisy jednego konta mają limit na minutę: zapis ponad limit dostaje 429
 * {@code application/problem+json} z Retry-After, a inne konto, odczyt i żądanie bez logowania
 * liczą się osobno albo wcale.
 *
 * <p>Wymagania: NFR-34</p>
 *
 * @since 0.1.0
 */
final class AccountRateLimitTest {

    @Test
    void refusesWriteOverLimitOfOneAccount() throws Exception {
        final AccountRateLimit limit = new AccountRateLimit(2, JsonMapper.builder().build());
        final List<Integer> statuses = new ArrayList<>(3);
        for (int idx = 0; idx < 2; idx += 1) {
            statuses.add(AccountRateLimitTest.send(limit, "POST", "konto-a").getStatus());
        }
        final MockHttpServletResponse refused = AccountRateLimitTest.send(limit, "PUT", "konto-a");
        MatcherAssert.assertThat(
            "Dwa zapisy nie przeszły albo trzeci nie dał 429 z Retry-After problem+json",
            "%s %d %b %s".formatted(
                statuses, refused.getStatus(),
                refused.getHeader(HttpHeaders.RETRY_AFTER) != null, refused.getContentType()
            ),
            Matchers.equalTo("[200, 200] 429 true application/problem+json")
        );
    }

    @Test
    void countsOtherAccountReadAndAnonymousSeparately() throws Exception {
        final AccountRateLimit limit = new AccountRateLimit(1, JsonMapper.builder().build());
        AccountRateLimitTest.send(limit, "DELETE", "konto-a");
        MatcherAssert.assertThat(
            "Inne konto, odczyt albo żądanie bez logowania zostało odrzucone limitem zapisów",
            List.of(
                AccountRateLimitTest.send(limit, "POST", "konto-b").getStatus(),
                AccountRateLimitTest.send(limit, "GET", "konto-a").getStatus(),
                AccountRateLimitTest.send(limit, "POST", null).getStatus()
            ),
            Matchers.contains(200, 200, 200)
        );
    }

    private static MockHttpServletResponse send(
        final AccountRateLimit limit, final String method, final @Nullable String account
    ) throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest(method, "/submissions");
        if (account != null) {
            request.setUserPrincipal(() -> account);
        }
        final MockHttpServletResponse response = new MockHttpServletResponse();
        limit.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
