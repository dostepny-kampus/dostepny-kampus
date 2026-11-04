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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
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
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

/**
 * Ustawienia magazynu zdjęć (ADR-0009): plik {@code application.yaml}
 * ze zmiennymi środowiskowymi, jeden rekord {@code dk.photos}, czytelny
 * błąd startu przy złej wartości i klucze ukryte w tekście (NFR-35).
 *
 * @since 0.1.0
 */
final class PhotoSettingsTest {

    @Test
    void cfgFourStartsWithDefaultsWithoutPhotoKeys() {
        PhotoSettingsTest.runner().run(
            context -> Assertions.assertAll(
                () -> Assertions.assertEquals(
                    new PhotoSettings(URI.create("http://127.0.0.1:9000"), "zdjecia", "", ""),
                    context.getBean(PhotoSettings.class),
                    "Bez zmiennych DK_PHOTOS_* serwer nie wstał z domyślnym magazynem bez kluczy"
                ),
                () -> Assertions.assertEquals(
                    List.of("jdbc:postgresql://127.0.0.1:5432/dostepny_kampus", "true"),
                    PhotoSettingsTest.properties(
                        context, "spring.datasource.url", "spring.mvc.problemdetails.enabled"
                    ),
                    "Bez zmiennych adres bazy nie jest domyślny albo błędy nie są ProblemDetail"
                )
            )
        );
    }

    @Test
    void cfgOneReadsPhotoEnvironmentVariables() {
        PhotoSettingsTest.runner().withPropertyValues(
            "DK_PHOTOS_HOST=magazyn", "DK_PHOTOS_PORT=9100", "DK_PHOTOS_BUCKET=zdjecia-testowe",
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
                ),
                () -> Assertions.assertEquals(
                    List.of("klucz-testowy", "sekret-testowy"),
                    PhotoSettingsTest.credentials(context.getBean(S3Client.class)),
                    "Klient S3 nie dostał klucza i sekretu z DK_PHOTOS_* na ich miejscach"
                )
            )
        );
    }

    @Test
    void cfgTwoWritesPhotoToBucketFromSettings() {
        final PhotoSettingsTest.BucketProbe probe = new PhotoSettingsTest.BucketProbe();
        final UUID photo = UUID.randomUUID();
        final ApplicationContextRunner runner = PhotoSettingsTest.runner()
            .withPropertyValues("DK_PHOTOS_BUCKET=zdjecia-testowe")
            .withBean(S3Client.class, () -> probe, bean -> bean.setPrimary(true));
        runner.run(
            context -> {
                context.getBean(PhotoStore.class).put(photo, photo, new byte[] {1});
                Assertions.assertEquals(
                    List.of("zdjecia-testowe"), probe.written,
                    "Magazyn zapisał zdjęcie poza kubełkiem z DK_PHOTOS_BUCKET"
                );
            }
        );
    }

    @Test
    void cfgOneReadsDatabaseEnvironmentVariables() {
        PhotoSettingsTest.runner().withPropertyValues(
            "DK_DB_HOST=baza", "DK_DB_PORT=5433", "DK_DB_NAME=kampus",
            "DK_DB_USER=uzytkownik", "DK_DB_PASSWORD=haslo"
        ).run(
            context -> Assertions.assertEquals(
                List.of("jdbc:postgresql://baza:5433/kampus", "uzytkownik", "haslo"),
                PhotoSettingsTest.properties(
                    context, "spring.datasource.url",
                    "spring.datasource.username", "spring.datasource.password"
                ),
                "Adres, użytkownik albo hasło bazy nie wzięły zmiennych DK_DB_*"
            )
        );
    }

    @Disabled("Ustawienia serwera są jeszcze w application.properties")
    @Test
    void cfgOneKeepsSettingsOnlyInYaml() {
        Assertions.assertEquals(
            List.of(true, false),
            Stream.of("application.yaml", "application.properties")
                .map(name -> new ClassPathResource(name).exists()).toList(),
            "Ustawienia serwera nie są wyłącznie w application.yaml"
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
                "DK_PHOTOS_HOST=", "dk.photos.endpoint=magazyn.local/zdjecia",
                "dk.photos.endpoint=http:zdjecia", "dk.photos.endpoint=http:///zdjecia"
            ).map(
                property -> () -> PhotoSettingsTest.runner().withPropertyValues(property).run(
                    context -> MatcherAssert.assertThat(
                        String.format("Adres %s nie zatrzymał startu", property),
                        PhotoSettingsTest.failure(context),
                        Matchers.containsString("dk.photos.endpoint")
                    )
                )
            )
        );
    }

    @Disabled("Tekst rekordu ustawień magazynu zawiera jeszcze klucze")
    @Test
    void cfgFiveHidesKeysInSettingsText() {
        MatcherAssert.assertThat(
            "Tekst ustawień zawiera klucz dostępu albo sekret magazynu",
            new PhotoSettings(
                URI.create("http://127.0.0.1:9000"), "zdjecia", "klucz-ukryty", "sekret-ukryty"
            ).toString(),
            Matchers.not(Matchers.matchesRegex(".*(klucz|sekret)-ukryty.*"))
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

    private static List<String> properties(
        final AssertableApplicationContext context, final String... keys
    ) {
        return Stream.of(keys).map(context.getEnvironment()::getProperty).toList();
    }

    private static List<String> credentials(final S3Client client) {
        final AwsCredentialsIdentity identity = client.serviceClientConfiguration()
            .credentialsProvider().resolveIdentity().join();
        return List.of(identity.accessKeyId(), identity.secretAccessKey());
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

    /**
     * Klient S3, który zapamiętuje kubełki zapisów zamiast je wykonywać.
     *
     * @since 0.1.0
     */
    private static final class BucketProbe implements S3Client {

        /**
         * Kubełki kolejnych zapisów.
         */
        private final List<String> written = new ArrayList<>(1);

        @Override
        public String serviceName() {
            return S3Client.SERVICE_NAME;
        }

        @Override
        public void close() {
            this.written.clear();
        }

        @Override
        public PutObjectResponse putObject(final PutObjectRequest request, final RequestBody body) {
            this.written.add(request.bucket());
            return PutObjectResponse.builder().build();
        }
    }
}
