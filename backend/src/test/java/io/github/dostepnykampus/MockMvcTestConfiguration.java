/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import jakarta.servlet.Filter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Jedno {@code MockMvc} dla wszystkich testów HTTP.
 *
 * <p>Każda odpowiedź przechodzi przez {@link OpenApiContract}, więc test nie
 * dopisuje tej kontroli sam i nie może jej pominąć. Żądania idą przez filtry
 * zabezpieczeń serwera, więc operacja z rolą w kontrakcie wymaga w teście
 * tożsamości, np. {@link AsCollector}.</p>
 *
 * @since 0.1.0
 */
@TestConfiguration(proxyBeanMethods = false)
public final class MockMvcTestConfiguration {

    /**
     * Tworzy konfigurację; konstruktor wywołuje tylko Spring przez refleksję.
     */
    private MockMvcTestConfiguration() {
    }

    /**
     * Wspólne {@code MockMvc} z kontrolą kontraktu przy każdej odpowiedzi.
     *
     * @param context Kontekst aplikacji z kontrolerami
     * @param security Filtry zabezpieczeń serwera
     * @return Klient żądań do kontrolerów aplikacji
     */
    @Bean
    static MockMvc mvc(
        final WebApplicationContext context,
        @Qualifier("springSecurityFilterChain") final Filter security
    ) {
        return MockMvcBuilders.webAppContextSetup(context)
            .addFilters(security)
            .alwaysExpect(new OpenApiContract())
            .build();
    }
}
