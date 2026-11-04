/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.IntegrationTest;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;

/**
 * Test I2b-4 (ADR-0009): kubełek zdjęć istnieje po starcie kontekstu, bez
 * żadnego zapisu zdjęcia. Własna nazwa kubełka daje własny kontekst i kontener,
 * więc inny test nie utworzy kubełka wcześniej.
 *
 * @since 0.1.0
 */
@IntegrationTest
@Import(RustfsTestConfiguration.class)
@TestPropertySource(properties = "dk.photos.bucket=zdjecia-po-starcie")
final class PhotoBucketStartupTest {

    @Test
    void iTwoBFourCreatesBucketAtStartup(final ApplicationContext context) {
        MatcherAssert.assertThat(
            "Po starcie kontekstu nie ma kubełka zdjęć",
            context.getBean(S3Client.class).listBuckets().buckets().stream()
                .map(Bucket::name).toList(),
            Matchers.hasItem("zdjecia-po-starcie")
        );
    }
}
