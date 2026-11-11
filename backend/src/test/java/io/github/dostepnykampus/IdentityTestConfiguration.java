/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Tożsamość testowa zamiast Usługi logowania.
 *
 * <p>Token to nazwa roli z kontraktu, np. {@code COLLECTOR}; każdy inny napis
 * dekoder odrzuca. Podpisu, wystawcy i ważności tokenu testy tu nie sprawdzają.
 * Punkty HTTP tylko dla testów są poza kontraktem, więc mają osobny łańcuch
 * bez zabezpieczeń; bez niego serwer by je odrzucał.</p>
 *
 * @since 0.1.0
 */
@TestConfiguration(proxyBeanMethods = false)
public final class IdentityTestConfiguration {

    /**
     * Role z kontraktu, czyli jedyne poprawne tokeny testowe.
     */
    private static final Set<String> ROLES = Set.of(
        "VISITOR", "COLLECTOR", "VERIFIER", "COORDINATOR"
    );

    /**
     * Tworzy konfigurację; konstruktor wywołuje tylko Spring przez refleksję.
     */
    private IdentityTestConfiguration() {
    }

    /**
     * Dekoder tokenów testowych.
     *
     * @return Dekoder, który przyjmuje nazwę roli jako token
     */
    @Bean
    static JwtDecoder decoder() {
        return token -> {
            if (!IdentityTestConfiguration.ROLES.contains(token)) {
                throw new BadJwtException("Nieznany token testowy");
            }
            return Jwt.withTokenValue(token).header("alg", "none").subject("konto-testowe")
                .claim("realm_access", Map.of("roles", List.of(token))).build();
        };
    }

    /**
     * Otwarty łańcuch dla punktów HTTP tylko dla testów.
     *
     * @param http Budowa zabezpieczeń żądań
     * @return Otwarty łańcuch przed łańcuchem API
     * @throws Exception Gdy konfiguracji nie da się zbudować
     */
    @Bean
    @Order(0)
    static SecurityFilterChain probes(final HttpSecurity http) throws Exception {
        http.securityMatcher("/proba-bledow/**", "/proba-wiazania");
        http.csrf(AbstractHttpConfigurer::disable);
        http.authorizeHttpRequests(rules -> rules.anyRequest().permitAll());
        return http.build();
    }
}
