/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.barrier.submission.content.SubmissionFloor;
import io.github.dostepnykampus.barrier.submission.content.SubmissionPlace;
import io.github.dostepnykampus.barrier.submission.content.SubmissionPoint;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Lista zgłoszeń stronami: odpowiedź ma najwyżej {@code limit} zgłoszeń, a przejście po
 * nagłówku {@code Link} z {@code rel="next"} daje całą listę bez powtórzeń.
 *
 * <p>Wymagania: NFR-34, US1.3</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class SubmissionPagesHttpIT {

    /**
     * Adres strony z nagłówka Link z relacją next.
     */
    private static final Pattern NEXT = Pattern.compile("<([^>]+)>; *rel=\"next\"");

    @Test
    void walksAllPagesWithinLimit(final WebApplicationContext context) throws Exception {
        final String floor = "strona-".concat(UUID.randomUUID().toString());
        final List<UUID> readable = Stream.generate(
            () -> SubmissionPagesHttpIT.add(context, floor)
        ).limit(5).toList();
        context.getBean(JdbcClient.class).sql(
            "UPDATE submissions SET content = content || '{\"photoIds\": \"zepsute\"}' WHERE id = :id"
        ).param("id", SubmissionPagesHttpIT.add(context, floor)).update();
        final List<String> seen = new ArrayList<>(6);
        final List<Integer> sizes = new ArrayList<>(4);
        @Nullable URI page = URI.create(
            String.format("/submissions?buildingSymbol=A-1&floorId=%s&limit=2", floor)
        );
        while (page != null) {
            final MockHttpServletResponse response = context.getBean(MockMvc.class)
                .perform(MockMvcRequestBuilders.get(page)).andReturn().getResponse();
            final JsonNode items = context.getBean(ObjectMapper.class).readTree(
                response.getContentAsString(StandardCharsets.UTF_8)
            );
            sizes.add(items.size());
            items.forEach(item -> seen.add(item.path("id").asString()));
            page = SubmissionPagesHttpIT.next(response.getHeader(HttpHeaders.LINK));
        }
        Assertions.assertAll(
            () -> Assertions.assertTrue(
                sizes.stream().allMatch(size -> size <= 2),
                String.format("Strona przekroczyła limit 2: %s", sizes)
            ),
            () -> Assertions.assertEquals(
                readable.stream().map(UUID::toString).collect(Collectors.toSet()),
                Set.copyOf(seen),
                "Strony nie dały wszystkich czytelnych zgłoszeń"
            ),
            () -> Assertions.assertEquals(
                seen.size(), Set.copyOf(seen).size(),
                String.format("Strony powtórzyły zgłoszenie: %s", seen)
            )
        );
    }

    private static @Nullable URI next(final @Nullable String link) {
        @Nullable URI result = null;
        if (link != null) {
            final Matcher matcher = SubmissionPagesHttpIT.NEXT.matcher(link);
            if (matcher.find()) {
                result = URI.create(matcher.group(1));
            }
        }
        return result;
    }

    private static UUID add(final WebApplicationContext context, final String floor) {
        return new SampleSubmissions(context).add(
            "ACCEPTED",
            SampleSubmissions.quick(
                new SubmissionPlace(
                    new SubmissionPoint(17.062, 51.108), null, false, "A-1",
                    new SubmissionFloor(floor, null, false)
                ),
                "Próg przy wejściu", UUID.randomUUID()
            )
        );
    }
}
