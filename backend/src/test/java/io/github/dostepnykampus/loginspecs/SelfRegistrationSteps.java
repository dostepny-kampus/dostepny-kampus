/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.loginspecs;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;
import io.github.dostepnykampus.KeycloakTestConfiguration;
import io.github.dostepnykampus.PostgisTestConfiguration;
import io.github.dostepnykampus.RustfsTestConfiguration;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import tools.jackson.databind.json.JsonMapper;

/**
 * Kroki scenariusza o samodzielnej rejestracji i ich kontekst Springa: serwer
 * z domyślnym dekoderem tokenów i Keycloak w kontenerze z realmem dewelopera.
 *
 * <p>Wymagania: US14.3, BR-1, BR-2</p>
 *
 * @since 0.1.0
 */
@CucumberContextConfiguration
@Tag("kontekst-springa")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(
    {
        PostgisTestConfiguration.class,
        RustfsTestConfiguration.class,
        KeycloakTestConfiguration.class
    }
)
public final class SelfRegistrationSteps {

    /** Ustawienia kontekstu: port serwera i adres realmu. */
    private final Environment settings;

    /** Czytnik odpowiedzi JSON. */
    private final JsonMapper mapper;

    /** Token zalogowanej osoby; pusty do zalogowania. */
    private String token;

    /**
     * Kroki na kontekście aplikacji.
     *
     * @param settings Ustawienia kontekstu
     * @param mapper Czytnik odpowiedzi JSON
     */
    public SelfRegistrationSteps(final Environment settings, final JsonMapper mapper) {
        this.settings = settings;
        this.mapper = mapper;
        this.token = "";
    }

    /** Krok aplikacji; serwer pilnuje tylko, że osoba nie ma jeszcze tokenu. */
    @Given("osoba bez konta korzysta z aplikacji jako Gość niezalogowany")
    public void guest() {
        MatcherAssert.assertThat("Token przed rejestracją", this.token, Matchers.emptyString());
    }

    /**
     * Osoba zakłada konto formularzem rejestracji realmu i loguje się.
     *
     * @throws Exception Gdy żądanie nie dojdzie do Keycloaka
     */
    @When("założy konto przez samodzielną rejestrację i zaloguje się")
    public void registers() throws Exception {
        final String realm = this.settings.getRequiredProperty(
            "spring.security.oauth2.resourceserver.jwt.issuer-uri"
        );
        final String name = "nowa-%s".formatted(UUID.randomUUID());
        try (HttpClient web = HttpClient.newHttpClient()) {
            final HttpResponse<String> page = web.send(
                HttpRequest.newBuilder(
                    URI.create(
                        "%s/protocol/openid-connect/registrations?response_type=code&scope=openid"
                            .concat("&client_id=dostepny-kampus-android&code_challenge_method=S256")
                            .concat("&code_challenge=E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM")
                            .concat("&redirect_uri=io.github.dostepnykampus%%3A%%2Foauth2redirect")
                            .formatted(realm)
                    )
                ).build(),
                HttpResponse.BodyHandlers.ofString()
            );
            final Matcher form = Pattern.compile("action=\"([^\"]+)\"").matcher(page.body());
            MatcherAssert.assertThat("Realm bez formularza", form.find(), Matchers.is(true));
            final HttpResponse<String> made = SelfRegistrationSteps.post(
                web, form.group(1).replace("&amp;", "&"),
                page.headers().allValues(HttpHeaders.SET_COOKIE).stream()
                    .map(cookie -> cookie.split(";", 2)[0]).collect(Collectors.joining("; ")),
                "username=%1$s&email=%1$s%%40example.test&firstName=Nowa&lastName=Osoba"
                    .concat("&password=%1$s&password-confirm=%1$s").formatted(name)
            );
            MatcherAssert.assertThat(
                "Formularz rejestracji nie założył konta: %s".formatted(made.body()),
                made.statusCode(), Matchers.equalTo(302)
            );
            this.token = this.mapper.readTree(
                SelfRegistrationSteps.post(
                    web, "%s/protocol/openid-connect/token".formatted(realm), "",
                    "grant_type=password&client_id=dostepny-kampus-android"
                        .concat("&username=%1$s&password=%1$s").formatted(name)
                ).body()
            ).path("access_token").asString();
        }
    }

    /**
     * Token nowego konta ma z ról kontraktu tylko rolę Odwiedzającego i tę
     * rolę pokazuje profil.
     *
     * @throws Exception Gdy żądanie nie dojdzie do serwera
     */
    @Then("w profilu widzi rolę „Odwiedzający”")
    public void seesVisitorRole() throws Exception {
        MatcherAssert.assertThat(
            "Realm nie dał nowemu kontu roli Odwiedzającego albo dał rolę pracowniczą",
            this.mapper.readTree(
                Base64.getUrlDecoder().decode(this.token.split("\\.", 3)[1])
            ).path("realm_access").path("roles").toString(),
            Matchers.matchesRegex("(?!.*(COLLECTOR|VERIFIER|COORDINATOR)).*\"VISITOR\".*")
        );
        MatcherAssert.assertThat(
            "Profil nowego konta ma role inne niż sama rola Odwiedzającego",
            this.mapper.readTree(this.call("/me", "").body()).path("roles").toString(),
            Matchers.equalTo("[\"VISITOR\"]")
        );
    }

    /**
     * Serwer przyjmuje zgłoszenie szybkie nowego konta.
     *
     * @throws Exception Gdy żądanie nie dojdzie do serwera
     */
    @Then("może złożyć zgłoszenie szybkie")
    public void sendsQuick() throws Exception {
        final HttpResponse<String> answer = this.call(
            "/submissions", "\"kind\":\"QUICK\",\"barrierGroup\":\"OTHER\""
        );
        MatcherAssert.assertThat(
            "Serwer nie przyjął zgłoszenia szybkiego nowego konta: %s".formatted(answer.body()),
            answer.statusCode(), Matchers.equalTo(201)
        );
    }

    /**
     * Serwer odmawia nowemu kontu czynności ról pracowniczych.
     *
     * @throws Exception Gdy żądanie nie dojdzie do serwera
     */
    @Then("nie widzi pomiaru precyzyjnego, kolejki weryfikacji ani postępu inwentaryzacji")
    public void staffRefused() throws Exception {
        MatcherAssert.assertThat(
            "Serwer nie odmówił nowemu kontu pomiaru, kolejki albo postępu",
            List.of(
                this.call(
                    "/submissions",
                    "\"kind\":\"PRECISE\",\"buildingSymbol\":\"C-13\",\"floor\":{\"number\":0},"
                        .concat("\"barrierGroup\":\"HORIZONTAL_CIRCULATION\",")
                        .concat("\"barrierType\":\"TB-13\",\"severity\":2")
                ).statusCode(),
                this.call("/review/queue", "").statusCode(),
                this.call("/progress", "").statusCode()
            ),
            Matchers.everyItem(Matchers.equalTo(403))
        );
    }

    private HttpResponse<String> call(final String path, final String fields) throws Exception {
        final HttpRequest.Builder request = HttpRequest.newBuilder(
            URI.create(
                "http://localhost:%s%s".formatted(
                    this.settings.getRequiredProperty("local.server.port"), path
                )
            )
        );
        if (!fields.isEmpty()) {
            request.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).POST(
                HttpRequest.BodyPublishers.ofString(
                    "{\"id\":\"%s\",%s,\"photoIds\":[\"%s\"],\"location\":{\"type\":\"Point\","
                        .concat("\"coordinates\":[17.06209,51.10785]}}")
                        .formatted(UUID.randomUUID(), fields, UUID.randomUUID())
                )
            );
        }
        request.header(HttpHeaders.AUTHORIZATION, "Bearer %s".formatted(this.token));
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        }
    }

    private static HttpResponse<String> post(
        final HttpClient web, final String address, final String cookies, final String fields
    ) throws Exception {
        return web.send(
            HttpRequest.newBuilder(URI.create(address))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .header(HttpHeaders.COOKIE, cookies)
                .POST(HttpRequest.BodyPublishers.ofString(fields)).build(),
            HttpResponse.BodyHandlers.ofString()
        );
    }
}
