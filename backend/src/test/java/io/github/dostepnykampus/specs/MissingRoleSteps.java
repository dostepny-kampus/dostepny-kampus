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
import java.util.function.Supplier;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Kroki szablonów o czynnościach niedostępnych bez wymaganej roli w aplikacji
 * i na serwerze.
 *
 * <p>Serwer sprawdza krok o próbie z pominięciem aplikacji. Kroki o ekranach
 * aplikacji pilnują tu tylko kolejności: sprawdza je test aplikacji.</p>
 *
 * <p>Wymagania: US14.1, BR-19, BR-20</p>
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
     * Zasób zgłoszeń w ścieżce HTTP.
     */
    private static final String SUBMISSIONS = "/submissions";

    /**
     * Nazwa czynności oznaczania piętra obejrzanego w scenariuszu.
     */
    private static final String FLOOR_MARKER = "znacznik piętra obejrzanego";

    /**
     * Fabryki żądań HTTP dla poszczególnych czynności ze scenariuszy.
     */
    private static final Map<String, Supplier<MockHttpServletRequestBuilder>> ATTEMPTS =
        Map.of(
            "pomiar precyzyjny",
            () -> MissingRoleSteps.jsonPost(
                MissingRoleSteps.SUBMISSIONS, MissingRoleSteps.precise()
            ),
            "zmiany oznaczenia wejścia",
            () -> MissingRoleSteps.jsonPost(
                MissingRoleSteps.SUBMISSIONS, MissingRoleSteps.marking()
            ),
            "zmiana »bariera usunięta«",
            () -> MissingRoleSteps.jsonPost(
                MissingRoleSteps.SUBMISSIONS, MissingRoleSteps.removed()
            ),
            MissingRoleSteps.FLOOR_MARKER,
            () -> MissingRoleSteps.jsonPost("/sync/push", MissingRoleSteps.marker()),
            "lista zgłoszeń",
            () -> MockMvcRequestBuilders.get(MissingRoleSteps.SUBMISSIONS)
                .param("mine", "true"),
            "kolejka weryfikacji",
            () -> MockMvcRequestBuilders.get("/review/queue"),
            "postęp inwentaryzacji",
            () -> MockMvcRequestBuilders.get("/progress"),
            "eksport",
            () -> MockMvcRequestBuilders.get("/export.geojson"),
            "zaproszenie",
            () -> MissingRoleSteps.jsonPost("/invitations", "{}")
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
     * Krok aplikacji; serwer pilnuje tylko kolejności kroków.
     */
    @When("przejrzy dostępne ekrany aplikacji")
    public void browsesScreens() {
        MatcherAssert.assertThat(
            "Krok aplikacji stoi przed logowaniem osoby",
            this.token, Matchers.not(Matchers.emptyString())
        );
    }

    /**
     * Krok aplikacji; serwer pilnuje tylko kolejności kroków.
     *
     * @param screen Nazwa ekranu ze scenariusza
     */
    @Then("^nie widzi (?:ekranu|czynności) „(.+)”$")
    public void screenHidden(final String screen) {
        MatcherAssert.assertThat(
            String.format("Krok o ekranie „%s” stoi przed logowaniem osoby", screen),
            this.token, Matchers.not(Matchers.emptyString())
        );
    }

    /**
     * Serwer odmawia czynności wysłanej wprost, bez aplikacji.
     *
     * @param action Nazwa czynności ze scenariusza
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @Then("^próba wykonania czynności „(.+)” z pominięciem aplikacji jest odrzucona$")
    @Then("^próba złożenia (zmiany oznaczenia wejścia) z pominięciem aplikacji jest odrzucona$")
    public void attemptRefused(final String action) throws Exception {
        MatcherAssert.assertThat(
            String.format("Serwer nie odmówił czynności „%s” roli %s", action, this.token),
            MissingRoleSteps.refusedStatus(
                action,
                this.http.perform(
                    MissingRoleSteps.attempt(action)
                        .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", this.token))
                ).andReturn().getResponse()
            ),
            Matchers.equalTo(403)
        );
    }

    private static int refusedStatus(
        final String action, final MockHttpServletResponse response
    ) throws Exception {
        final int status;
        if (MissingRoleSteps.FLOOR_MARKER.equals(action)
            && response.getStatus() == 200
            && response.getContentAsString().contains("\"REJECTED\"")
            && response.getContentAsString().contains("\"status\":403")) {
            status = 403;
        } else {
            status = response.getStatus();
        }
        return status;
    }

    private static MockHttpServletRequestBuilder attempt(final String action) {
        final Supplier<MockHttpServletRequestBuilder> builder =
            MissingRoleSteps.ATTEMPTS.get(action);
        if (builder == null) {
            throw new IllegalArgumentException(
                String.format("Scenariusz podaje czynność bez żądania: %s", action)
            );
        }
        return builder.get();
    }

    private static MockHttpServletRequestBuilder jsonPost(
        final String path, final String content
    ) {
        return MockMvcRequestBuilders.post(path)
            .contentType(MediaType.APPLICATION_JSON)
            .content(content);
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

    private static String marking() {
        return """
            {"id":"%s","kind":"ENTRANCE_MARKING","buildingSymbol":"B-8",\
            "entranceId":"node/12127546084","entranceMarking":"ACCESSIBLE",\
            "location":{"type":"Point","coordinates":[17.065862,51.1078003]},\
            "photoIds":["%s"]}""".formatted(UUID.randomUUID(), UUID.randomUUID());
    }

    private static String removed() {
        return """
            {"id":"%s","kind":"PRECISE","buildingSymbol":"C-13","floor":{"number":0},\
            "location":{"type":"Point","coordinates":[17.06209,51.10785]},\
            "barrierGroup":"HORIZONTAL_CIRCULATION","barrierType":"TB-13",\
            "barrierRemoved":true,"replacesSubmissionId":"%s","photoIds":["%s"]}"""
            .formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    }

    private static String marker() {
        return """
            {"operations":[{"clientOperationId":"%s","type":"SET_INSPECTION_MARKER",\
            "marker":{"id":"%s","buildingSymbol":"C-13"}}]}"""
            .formatted(UUID.randomUUID(), UUID.randomUUID());
    }
}
