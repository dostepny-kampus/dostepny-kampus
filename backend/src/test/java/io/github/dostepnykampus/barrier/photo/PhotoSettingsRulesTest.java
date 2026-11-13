/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.photo;

import java.net.URI;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Złe ustawienia magazynu zdjęć zatrzymują start z nazwą ustawienia
 * (ADR-0009): brak adresu, adres bez schematu, brak kubełka i kubełek
 * z samych spacji.
 *
 * @since 0.1.0
 */
final class PhotoSettingsRulesTest {

    @Test
    void cfgThreeRejectsMissingEndpointOrEndpointWithoutScheme() {
        Assertions.assertAll(
            () -> PhotoSettingsRulesTest.rejected(null, "zdjecia", "dk.photos.endpoint"),
            () -> PhotoSettingsRulesTest.rejected(
                URI.create("//magazyn:9000"), "zdjecia", "dk.photos.endpoint"
            )
        );
    }

    @Test
    void cfgThreeRejectsMissingOrBlankBucket() {
        Assertions.assertAll(
            () -> PhotoSettingsRulesTest.rejected(
                URI.create("http://magazyn:9000"), null, "dk.photos.bucket"
            ),
            () -> PhotoSettingsRulesTest.rejected(
                URI.create("http://magazyn:9000"), "  ", "dk.photos.bucket"
            )
        );
    }

    private static void rejected(final URI endpoint, final String bucket, final String setting) {
        MatcherAssert.assertThat(
            String.format("Adres %s i kubełek '%s' nie zatrzymały startu", endpoint, bucket),
            Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new PhotoSettings(endpoint, bucket, "", "")
            ).getMessage(),
            Matchers.containsString(setting)
        );
    }
}
