/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.AsVerifier;
import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.barrier.photo.PhotoStore;
import io.github.dostepnykampus.identity.api.AccountId;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Autor zgłoszenia widziany przez Weryfikatora w kolejce i po wzięciu: pseudonim i role
 * z chwili wysłania, bez identyfikatora konta; odczyt zgłoszenia poza weryfikacją autora
 * nie pokazuje.
 *
 * <p>Wymagania: BR-5</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class ReviewAuthorHttpIT {

    /**
     * Konto autora zgłoszeń.
     */
    private static final String ACCOUNT = "konto-autora";

    @Test
    void showsAuthorInQueue(final WebApplicationContext context) throws Exception {
        MatcherAssert.assertThat(
            "Kolejka nie pokazuje pseudonimu i ról autora z chwili wysłania",
            ReviewAuthorHttpIT.author(
                ReviewAuthorHttpIT.queued(
                    context, ReviewAuthorHttpIT.sent(context, "COLLECTOR")
                )
            ),
            Matchers.contains(
                new AccountId(ReviewAuthorHttpIT.ACCOUNT).pseudonym(), "[VISITOR, COLLECTOR]"
            )
        );
    }

    @Test
    void showsAuthorAfterClaimWithoutAccount(final WebApplicationContext context)
        throws Exception {
        final String body = context.getBean(MockMvc.class).perform(
            MockMvcRequestBuilders.post(
                "/submissions/{id}/claim",
                ReviewAuthorHttpIT.sent(context, "COLLECTOR+COORDINATOR")
            ).with(new AsVerifier())
        ).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        MatcherAssert.assertThat(
            "Wzięcie nie pokazuje autora albo ujawnia identyfikator jego konta",
            List.of(
                ReviewAuthorHttpIT.author(
                    context.getBean(ObjectMapper.class).readTree(body).path("author")
                ),
                body.contains(ReviewAuthorHttpIT.ACCOUNT)
            ),
            Matchers.contains(
                List.of(
                    new AccountId(ReviewAuthorHttpIT.ACCOUNT).pseudonym(),
                    "[VISITOR, COLLECTOR, COORDINATOR]"
                ),
                false
            )
        );
    }

    @Test
    void omitsAuthorOutsideReview(final WebApplicationContext context) throws Exception {
        MatcherAssert.assertThat(
            "Odczyt zgłoszenia poza operacjami Weryfikatora pokazuje autora",
            ReviewAuthorHttpIT.json(
                context,
                MockMvcRequestBuilders.get(
                    "/submissions/{id}", ReviewAuthorHttpIT.sent(context, "COLLECTOR")
                ).with(new AsVerifier())
            ).has("author"),
            Matchers.is(false)
        );
    }

    private static UUID sent(final WebApplicationContext context, final String roles)
        throws Exception {
        final UUID id = UUID.randomUUID();
        final UUID photo = UUID.randomUUID();
        context.getBean(MockMvc.class).perform(
            MockMvcRequestBuilders.post("/submissions")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, ReviewAuthorHttpIT.token(roles))
                .content(ReviewAuthorHttpIT.quick(id, photo))
        );
        context.getBean(PhotoStore.class).put(id, photo, JpegCleanerTest.photo());
        return id;
    }

    private static String token(final String roles) {
        return String.format("Bearer %s.%s", roles, ReviewAuthorHttpIT.ACCOUNT);
    }

    private static String quick(final UUID id, final UUID photo) {
        return String.join(
            "",
            "{\"id\":\"", id.toString(), "\",\"kind\":\"QUICK\",",
            "\"barrierGroup\":\"OTHER\",",
            "\"location\":{\"type\":\"Point\",\"coordinates\":[17.06209,51.10785]},",
            "\"photoIds\":[\"", photo.toString(), "\"]}"
        );
    }

    private static JsonNode queued(final WebApplicationContext context, final UUID id)
        throws Exception {
        return StreamSupport.stream(
            ReviewAuthorHttpIT.json(
                context, MockMvcRequestBuilders.get("/review/queue").with(new AsVerifier())
            ).spliterator(),
            false
        ).filter(item -> id.toString().equals(item.path("id").asString()))
            .findFirst().orElseThrow().path("author");
    }

    private static JsonNode json(
        final WebApplicationContext context, final RequestBuilder request
    ) throws Exception {
        return context.getBean(ObjectMapper.class).readTree(
            context.getBean(MockMvc.class).perform(request)
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)
        );
    }

    private static List<String> author(final JsonNode author) {
        return List.of(
            author.path("pseudonym").asString(),
            StreamSupport.stream(author.path("roles").spliterator(), false)
                .map(JsonNode::asString).toList().toString()
        );
    }
}
