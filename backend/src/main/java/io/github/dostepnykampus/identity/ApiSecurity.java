/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.identity;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.json.JsonMapper;

/**
 * Zabezpieczenia API: zabronione wszystko, czego kontrakt
 * {@code api/openapi.yaml} nie dopuszcza (NFR-33, US14.1, ADR-0003).
 *
 * <p>Serwer jest serwerem zasobów OAuth 2.0 bez własnego logowania; role są
 * w {@code realm_access.roles} tokenu. Operacje publiczne nie wymagają tokenu.
 * Pozostałe bez ważnego tokenu dają 401, a bez wymaganej roli 403, obie jako
 * {@code application/problem+json}. Ścieżka spoza tabeli jest odrzucana.
 * Zgodność tabeli z kontraktem sprawdza {@code SecurityContractTest}.</p>
 *
 * @since 0.1.0
 */
@Configuration(proxyBeanMethods = false)
public final class ApiSecurity {

    /**
     * Tworzy konfigurację; konstruktor wywołuje tylko Spring przez refleksję.
     */
    private ApiSecurity() {
    }

    /**
     * Zabezpieczenia API bez sesji: reguły dostępu, token JWT i odmowy.
     *
     * @param http Budowa zabezpieczeń żądań
     * @param mapper Zapis odpowiedzi odmowy
     * @return Zbudowany łańcuch dla wszystkich żądań
     * @throws Exception Gdy konfiguracji nie da się zbudować
     */
    @Bean
    static SecurityFilterChain api(
        final HttpSecurity http, final JsonMapper mapper
    ) throws Exception {
        final JwtAuthenticationConverter roles = new JwtAuthenticationConverter();
        roles.setJwtGrantedAuthoritiesConverter(ApiSecurity::authorities);
        http.csrf(AbstractHttpConfigurer::disable);
        http.sessionManagement(
            sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );
        http.authorizeHttpRequests(ApiSecurity::rules);
        http.oauth2ResourceServer(
            server -> {
                server.jwt(jwt -> jwt.jwtAuthenticationConverter(roles));
                server.authenticationEntryPoint(
                    (request, response, error) ->
                        ApiSecurity.reject(response, HttpStatus.UNAUTHORIZED, mapper)
                );
                server.accessDeniedHandler(
                    (request, response, error) ->
                        ApiSecurity.reject(response, HttpStatus.FORBIDDEN, mapper)
                );
            }
        );
        return http.build();
    }

    private static void rules(
        final AuthorizeHttpRequestsConfigurer<HttpSecurity>
            .AuthorizationManagerRequestMatcherRegistry rules
    ) {
        rules.requestMatchers(
            HttpMethod.GET, "/buildings", "/buildings/locate", "/buildings/{symbol}",
            "/buildings/{symbol}/floors", "/buildings/{symbol}/floors/{floor}/rooms",
            "/rooms", "/submissions", "/submissions/{id}",
            "/submissions/{id}/photos/{photo}"
        ).permitAll();
        rules.requestMatchers(HttpMethod.PUT, "/submissions/{id}").hasRole("COLLECTOR");
        rules.requestMatchers(HttpMethod.GET, "/review/queue", "/submissions/{id}/similar")
            .hasRole("VERIFIER");
        rules.requestMatchers(
            HttpMethod.POST, "/submissions/{id}/claim", "/submissions/{id}/review"
        ).hasRole("VERIFIER");
        rules.requestMatchers(HttpMethod.DELETE, "/inspection-markers/{id}")
            .hasAnyRole("COLLECTOR", "COORDINATOR");
        rules.requestMatchers(HttpMethod.GET, "/progress", "/export.geojson", "/export.csv")
            .hasRole("COORDINATOR");
        rules.requestMatchers(HttpMethod.POST, "/invitations").hasRole("COORDINATOR");
        rules.requestMatchers(HttpMethod.POST, "/submissions", "/sync/push").authenticated();
        rules.requestMatchers(HttpMethod.PUT, "/submissions/{id}/photos/{photo}").authenticated();
        rules.requestMatchers(HttpMethod.GET, "/sync/pull", "/me").authenticated();
        rules.anyRequest().denyAll();
    }

    private static Collection<GrantedAuthority> authorities(final Jwt token) {
        final Map<String, Object> realm = Objects.requireNonNullElse(
            token.getClaimAsMap("realm_access"), Map.of()
        );
        Collection<?> roles = List.of();
        if (realm.get("roles") instanceof Collection<?> listed) {
            roles = listed;
        }
        return roles.stream()
            .<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_%s".formatted(role)))
            .toList();
    }

    private static void reject(
        final HttpServletResponse response, final HttpStatus status, final JsonMapper mapper
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        if (status == HttpStatus.UNAUTHORIZED) {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        }
        mapper.writeValue(
            response.getOutputStream(),
            Map.of("title", status.getReasonPhrase(), "status", status.value())
        );
    }
}
