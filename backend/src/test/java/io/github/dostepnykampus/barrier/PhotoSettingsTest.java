/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Ustawienia magazynu zdjęć (ADR-0009): plik {@code application.yaml}
 * ze zmiennymi środowiskowymi, jeden rekord {@code dk.photos}, czytelny
 * błąd startu przy złej wartości i klucze ukryte w tekście (NFR-35).
 *
 * <p>Zmienne środowiskowe są tu właściwościami testu; prawdziwe
 * środowisko maszyny jest odłączone.</p>
 *
 * @since 0.1.0
 */
final class PhotoSettingsTest {

    @Test
    void cfgOneReadsPhotoDefaultsWithoutEnvironment() {
        PhotoSettingsTest.runner().run(
            context -> Assertions.assertAll(
                () -> Assertions.assertEquals(
                    "zdjecia",
                    context.getBean(PhotoSettings.class).bucket(),
                    "Bez DK_PHOTOS_BUCKET kubełek nie jest domyślnym zdjecia"
                ),
                () -> Assertions.assertEquals(
                    URI.create("http://127.0.0.1:9000"),
                    context.getBean(PhotoSettings.class).endpoint(),
                    "Bez zmiennych adres magazynu nie jest domyślnym"
                )
            )
        );
    }

    @Test
    void cfgOneKeepsServerDefaultsWithoutEnvironment() {
        PhotoSettingsTest.runner().run(
            context -> Assertions.assertAll(
                () -> Assertions.assertEquals(
                    "jdbc:postgresql://127.0.0.1:5432/dostepny_kampus",
                    context.getEnvironment().getProperty("spring.datasource.url"),
                    "Bez zmiennych adres bazy nie jest domyślnym"
                ),
                () -> Assertions.assertEquals(
                    "true",
                    context.getEnvironment().getProperty("spring.mvc.problemdetails.enabled"),
                    "Odpowiedzi błędów przestały być ProblemDetail"
                )
            )
        );
    }

    @Test
    void cfgOneReadsPhotoEnvironmentVariables() {
        PhotoSettingsTest.runner().withPropertyValues(
            "DK_PHOTOS_HOST=magazyn", "DK_PHOTOS_PORT=9100",
            "DK_PHOTOS_BUCKET=zdjecia-testowe",
            "DK_PHOTOS_ACCESS_KEY=klucz-testowy", "DK_PHOTOS_SECRET_KEY=sekret-testowy"
        ).run(
            context -> Assertions.assertAll(
                () -> Assertions.assertEquals(
                    new PhotoSettings(
                        URI.create("http://magazyn:9100"), "zdjecia-testowe",
                        "klucz-testowy", "sekret-testowy"
                    ),
                    context.getBean(PhotoSettings.class),
                    "Ustawienia magazynu nie wzięły zmiennych DK_PHOTOS_*"
                ),
                () -> Assertions.assertEquals(
                    Optional.of(URI.create("http://magazyn:9100")),
                    context.getBean(S3Client.class).serviceClientConfiguration().endpointOverride(),
                    "Klient S3 łączy się pod inny adres niż z DK_PHOTOS_HOST i DK_PHOTOS_PORT"
                )
            )
        );
    }

    @Test
    void cfgOneReadsDatabaseEnvironmentVariables() {
        PhotoSettingsTest.runner().withPropertyValues(
            "DK_DB_HOST=baza", "DK_DB_PORT=5433", "DK_DB_NAME=kampus",
            "DK_DB_USER=uzytkownik", "DK_DB_PASSWORD=haslo"
        ).run(
            context -> Assertions.assertAll(
                () -> Assertions.assertEquals(
                    "jdbc:postgresql://baza:5433/kampus",
                    context.getEnvironment().getProperty("spring.datasource.url"),
                    "Adres bazy nie wziął DK_DB_HOST, DK_DB_PORT i DK_DB_NAME"
                ),
                () -> Assertions.assertEquals(
                    "uzytkownik",
                    context.getEnvironment().getProperty("spring.datasource.username"),
                    "Użytkownik bazy nie wziął DK_DB_USER"
                ),
                () -> Assertions.assertEquals(
                    "haslo",
                    context.getEnvironment().getProperty("spring.datasource.password"),
                    "Hasło bazy nie wzięło DK_DB_PASSWORD"
                )
            )
        );
    }

    @Disabled("Ustawienia serwera są jeszcze w application.properties")
    @Test
    void cfgOneKeepsSettingsOnlyInYaml() {
        Assertions.assertAll(
            () -> Assertions.assertTrue(
                new ClassPathResource("application.yaml").exists(),
                "Brak pliku application.yaml z ustawieniami serwera"
            ),
            () -> Assertions.assertFalse(
                new ClassPathResource("application.properties").exists(),
                "Plik application.properties nadal nadpisuje ustawienia z YAML"
            )
        );
    }

    @Disabled("Moduł zgłoszeń czyta jeszcze ustawienia magazynu przez @Value")
    @Test
    void cfgTwoReadsPhotoSettingsWithoutValueAnnotation() {
        Assertions.assertDoesNotThrow(
            () -> ArchRuleDefinition.noClasses()
                .should().dependOnClassesThat()
                .haveFullyQualifiedName("org.springframework.beans.factory.annotation.Value")
                .check(PhotoSettingsTest.barrier()),
            "Moduł zgłoszeń czyta ustawienia przez @Value zamiast rekordu dk.photos"
        );
    }

    @Disabled("Rekord ustawień magazynu przyjmuje jeszcze pusty kubełek")
    @Test
    void cfgThreeStopsStartupOnEmptyBucket() {
        PhotoSettingsTest.runner().withPropertyValues("DK_PHOTOS_BUCKET=").run(
            context -> MatcherAssert.assertThat(
                "Pusty kubełek nie zatrzymał startu z nazwą ustawienia",
                PhotoSettingsTest.failure(context),
                Matchers.containsString("dk.photos.bucket")
            )
        );
    }

    @Disabled("Rekord ustawień magazynu przyjmuje jeszcze adres bez schematu lub hosta")
    @Test
    void cfgThreeStopsStartupOnEndpointWithoutSchemeOrHost() {
        Assertions.assertAll(
            Stream.of(
                "DK_PHOTOS_HOST=",
                "dk.photos.endpoint=magazyn.local/zdjecia",
                "dk.photos.endpoint=http:zdjecia",
                "dk.photos.endpoint=http:///zdjecia"
            ).map(
                property -> () -> PhotoSettingsTest.runner().withPropertyValues(property).run(
                    context -> MatcherAssert.assertThat(
                        String.format(
                            "Adres bez schematu lub hosta (%s) nie zatrzymał startu", property
                        ),
                        PhotoSettingsTest.failure(context),
                        Matchers.containsString("dk.photos.endpoint")
                    )
                )
            )
        );
    }

    @Test
    void cfgFourStartsWithoutPhotoKeys() {
        PhotoSettingsTest.runner().withPropertyValues(
            "DK_PHOTOS_ACCESS_KEY=", "DK_PHOTOS_SECRET_KEY="
        ).run(
            context -> Assertions.assertEquals(
                "",
                PhotoSettingsTest.failure(context),
                "Serwer bez kluczy magazynu nie wystartował"
            )
        );
    }

    @Disabled("Tekst rekordu ustawień magazynu zawiera jeszcze klucze")
    @Test
    void cfgFiveHidesKeysInSettingsText() {
        final String text = new PhotoSettings(
            URI.create("http://127.0.0.1:9000"), "zdjecia",
            "klucz-ukryty", "sekret-ukryty"
        ).toString();
        Assertions.assertAll(
            () -> MatcherAssert.assertThat(
                "Tekst ustawień zawiera sekret magazynu",
                text, Matchers.not(Matchers.containsString("sekret-ukryty"))
            ),
            () -> MatcherAssert.assertThat(
                "Tekst ustawień zawiera klucz dostępu magazynu",
                text, Matchers.not(Matchers.containsString("klucz-ukryty"))
            )
        );
    }

    private static ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
            .withInitializer(PhotoSettingsTest::detachSystemEnvironment)
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(PhotoSettingsTest.Photos.class);
    }

    private static void detachSystemEnvironment(final ConfigurableApplicationContext context) {
        context.getEnvironment().getPropertySources().remove(
            StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME
        );
    }

    private static String failure(final AssertableApplicationContext context) {
        return Stream.iterate(context.getStartupFailure(), Objects::nonNull, Throwable::getCause)
            .map(String::valueOf)
            .collect(Collectors.joining(" <- "));
    }

    private static JavaClasses barrier() {
        return new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackagesOf(PhotoSettings.class);
    }

    /**
     * Magazyn zdjęć z modułu zgłoszeń z rekordem ustawień {@code dk.photos}.
     *
     * @since 0.1.0
     */
    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(PhotoSettings.class)
    @Import({PhotoStorage.class, PhotoStore.class})
    static final class Photos {

        private Photos() {
        }
    }
}
