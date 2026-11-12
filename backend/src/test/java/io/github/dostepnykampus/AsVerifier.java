/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Tożsamość Weryfikatora dla żądania w teście HTTP.
 *
 * <p>Dopisuje token testowy, który {@link IdentityTestConfiguration}
 * zamienia na rolę {@code VERIFIER}. Testy operacji wymagających w
 * kontrakcie zalogowania albo tej roli dodają ją do żądania.</p>
 *
 * @since 0.1.0
 */
public final class AsVerifier implements RequestPostProcessor {

    /**
     * Tożsamość Weryfikatora.
     */
    public AsVerifier() {
        // Bez stanu: token testowy jest stały.
    }

    @Override
    public MockHttpServletRequest postProcessRequest(final MockHttpServletRequest request) {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer VERIFIER");
        return request;
    }
}
