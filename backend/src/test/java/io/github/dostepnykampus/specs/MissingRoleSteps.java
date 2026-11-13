/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.specs;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.Map;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Kroki szablonu „Bez wymaganej roli czynność jest niedostępna w aplikacji
 * i na serwerze” (US14.1).
 *
 * <p>Serwer sprawdza krok o próbie z pominięciem aplikacji. Kroki o ekranach
 * aplikacji nie mają tu działania: sprawdza je test aplikacji.</p>
 *
 * @since 0.1.0
 */
public final class MissingRoleSteps {

    /**
     * Token testowy dla roli nazwanej w scenariuszu.
     */
    private static final Map<String, String> TOKENS = Map.of(
        "Odwiedzającego", "VISITOR", "Zbierającego", "COLLECTOR",
        "Weryfikatora", "VERIFIER", "Koordynatora", "COORDINATOR"
    );

    /**
     * Klient żądań HTTP do aplikacji.
     */
    private final MockMvc http;

    /**
     * Token zalogowanej osoby.
     */
    private String token;

    /**
     * Kroki na kontekście aplikacji.
     *
     * @param http Klient żądań HTTP do aplikacji
     */
    public MissingRoleSteps(final MockMvc http) {
        this.http = http;
        this.token = "";
    }

    /**
     * Osoba ma jedną rolę.
     *
     * @param role Nazwa roli w dopełniaczu, jak w scenariuszu
     */
    @Given("osoba jest zalogowana tylko z rolą {word}")
    public void loggedInAs(final String role) {
        this.token = MissingRoleSteps.TOKENS.get(role);
        MatcherAssert.assertThat(
            String.format("Scenariusz podaje nieznaną rolę: %s", role),
            this.token, Matchers.notNullValue()
        );
    }

    /**
     * Krok aplikacji; serwer nie ma tu działania.
     */
    @When("przejrzy dostępne ekrany aplikacji")
    public void browsesScreens() {
        // Ekrany sprawdza test aplikacji tego scenariusza.
    }

    /**
     * Krok aplikacji; serwer nie ma tu działania.
     *
     * @param screen Nazwa ekranu ze scenariusza
     */
    @Then("^nie widzi ekranu „(.+)”$")
    public void screenHidden(final String screen) {
        // Ekran sprawdza test aplikacji tego scenariusza.
    }

    /**
     * Serwer odmawia czynności wysłanej wprost, bez aplikacji.
     *
     * @param action Nazwa czynności ze scenariusza
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @Then("^próba wykonania czynności „(.+)” z pominięciem aplikacji jest odrzucona$")
    public void attemptRefused(final String action) throws Exception {
        MatcherAssert.assertThat(
            String.format("Serwer nie odmówił czynności „%s” roli %s", action, this.token),
            this.http.perform(
                MissingRoleSteps.attempt(action)
                    .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", this.token))
            ).andReturn().getResponse().getStatus(),
            Matchers.equalTo(403)
        );
    }

    private static MockHttpServletRequestBuilder attempt(final String action) {
        return switch (action) {
            case "pomiar precyzyjny" -> MockMvcRequestBuilders.post("/submissions")
                .contentType(MediaType.APPLICATION_JSON).content(MissingRoleSteps.precise());
            case "lista zgłoszeń" -> MockMvcRequestBuilders.get("/submissions")
                .param("mine", "true");
            case "kolejka weryfikacji" -> MockMvcRequestBuilders.get("/review/queue");
            case "postęp inwentaryzacji" -> MockMvcRequestBuilders.get("/progress");
            case "eksport" -> MockMvcRequestBuilders.get("/export.geojson");
            case "zaproszenie" -> MockMvcRequestBuilders.post("/invitations")
                .contentType(MediaType.APPLICATION_JSON).content("{}");
            default -> throw new IllegalArgumentException(
                String.format("Scenariusz podaje czynność bez żądania: %s", action)
            );
        };
    }

    private static String precise() {
        return String.join(
            "",
            "{\"id\":\"", UUID.randomUUID().toString(), "\",\"kind\":\"PRECISE\",",
            "\"location\":{\"type\":\"Point\",\"coordinates\":[17.06209,51.10785]},",
            "\"buildingSymbol\":\"C-13\",\"floor\":{\"number\":0},",
            "\"barrierGroup\":\"HORIZONTAL_CIRCULATION\",\"barrierType\":\"TB-13\",",
            "\"severity\":2,\"photoIds\":[\"", UUID.randomUUID().toString(), "\"]}"
        );
    }
}
