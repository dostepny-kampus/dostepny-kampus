/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.IntegrationTest;
import jakarta.servlet.ServletInputStream;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.DelegatingServletInputStream;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * NFR-34: HTTP odrzuca nadmiar przed odczytaniem całego strumienia i zapisem zdjęcia.
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class PhotoUploadLimitHttpTest {

    @ParameterizedTest
    @ValueSource(longs = {-1, 1, 5_000_001})
    void nfrThirtyFourBoundsReadBeforeRejecting(
        final long declared, final WebApplicationContext context
    ) throws Exception {
        final UUID id = PhotoUploadLimitHttpTest.add(context);
        final ByteArrayInputStream source = new ByteArrayInputStream(new byte[5_000_100]);
        final MockHttpServletResponse response = PhotoUploadLimitHttpTest.put(
            context, id, source, declared
        );
        int consumed = 5_000_001;
        if (declared > 5_000_000) {
            consumed = 0;
        }
        MatcherAssert.assertThat(
            "Przekroczenie limitu nie zatrzymuje odczytu albo zapisuje zdjęcie",
            List.of(
                response.getStatus(), 5_000_100 - source.available(),
                PhotoUploadLimitHttpTest.getStatus(context, id)
            ),
            Matchers.equalTo(List.of(413, consumed, 404))
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {4_999_999, 5_000_000})
    void nfrThirtyFourAcceptsUpToFiveMegabytes(
        final int size, final WebApplicationContext context
    ) throws Exception {
        MatcherAssert.assertThat(
            "Poprawne zdjęcie mieszczące się w limicie nie daje204",
            PhotoUploadLimitHttpTest.put(
                context, PhotoUploadLimitHttpTest.add(context),
                new ByteArrayInputStream(Arrays.copyOf(JpegCleanerTest.photo(), size)), size
            ).getStatus(),
            Matchers.equalTo(204)
        );
    }

    private static UUID add(final WebApplicationContext context) {
        final UUID id = UUID.randomUUID();
        context.getBean(JdbcClient.class).sql(
            """
            INSERT INTO submissions (id, status, kind, location, content)
            VALUES (:id, 'SENT', 'QUICK', ST_SetSRID(ST_MakePoint(17.0, 51.0), 4326),
                CAST(:content AS JSONB))
            """
        ).param("id", id).param(
            "content",
            """
            {"kind":"QUICK","location":{"type":"Point","coordinates":[17,51]},
            "barrierGroup":"OTHER","photoIds":["%s"]}
            """.formatted(id)
        ).update();
        return id;
    }

    private static MockHttpServletResponse put(
        final WebApplicationContext context, final UUID id,
        final ByteArrayInputStream source, final long declared
    ) throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest(
            context.getServletContext()
        ) {
            @Override
            public long getContentLengthLong() {
                return declared;
            }

            @Override
            public ServletInputStream getInputStream() {
                return new DelegatingServletInputStream(source);
            }
        };
        request.setMethod("PUT");
        request.setRequestURI("/submissions/%s/photos/%s".formatted(id, id));
        request.setContentType(MediaType.IMAGE_JPEG_VALUE);
        if (declared >= 0) {
            request.addHeader(HttpHeaders.CONTENT_LENGTH, declared);
        }
        return PhotoUploadLimitHttpTest.mvc(context).perform(servlet -> request)
            .andReturn().getResponse();
    }

    private static int getStatus(final WebApplicationContext context, final UUID id)
        throws Exception {
        context.getBean(JdbcClient.class)
            .sql("UPDATE submissions SET status = 'ACCEPTED' WHERE id = :id")
            .param("id", id).update();
        return PhotoUploadLimitHttpTest.mvc(context).perform(
            MockMvcRequestBuilders.get("/submissions/{id}/photos/{photo}", id, id)
        ).andReturn().getResponse().getStatus();
    }

    private static MockMvc mvc(final WebApplicationContext context) {
        return context.getBean(MockMvc.class);
    }
}
