/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Każda asercja w testach serwera ma komunikat: metody {@code assert…} z JUnit kończą się
 * parametrem {@code String} albo {@code Supplier}, a {@code assertThat} z Hamcrest zaczyna się
 * powodem. {@code assertAll} tylko grupuje asercje, więc komunikatu nie wymaga. Klasy, które dziś
 * łamią regułę, są na liście wyjątków z numerem issue; lista tylko maleje.
 *
 * @since 0.1.0
 */
final class AssertionMessageArchitectureTest {

    /**
     * Klasy testów z asercją bez komunikatu i issue, które to usuwa; nazwy bez wspólnego
     * początku pakietu.
     */
    private static final Map<String, Integer> KNOWN = Stream.of(
        "AbsentValueJsonTest",
        "MockMvcContractTest",
        "OpenApiContractTest",
        "barrier.SubmissionsWriteContractTest",
        "barrier.photo.PhotoUploadTest",
        "barrier.photo.storage.PhotoSettingsRulesTest",
        "barrier.photo.storage.PhotoSettingsTest",
        "barrier.photo.storage.PhotoStoreKeysTest",
        "barrier.submission.application.SubmissionServiceTest",
        "barrier.submission.content.json.ApiEnumsTest",
        "barrier.submission.content.json.CollectorContentJsonTest",
        "barrier.submission.persistence.JdbcErrorsTest",
        "barrier.submission.persistence.JdbcReadTest",
        "barrier.submission.persistence.JdbcRowTest",
        "barrier.submission.web.ReplacedSubmissionCheckTest"
    ).collect(
        Collectors.toMap(
            name -> String.format("io.github.dostepnykampus.%s", name), name -> 1961
        )
    );

    /**
     * Pakiet przykładów kompilowanych przez test.
     */
    private static final String SAMPLES = "io.github.dostepnykampus.archsamples.asserts";

    @Test
    void assertionsCarryMessages() {
        Assertions.assertEquals(
            List.of(),
            AssertionMessageArchitectureTest.violators(AssertionMessageArchitectureTest.tests())
                .stream()
                .filter(name -> !AssertionMessageArchitectureTest.KNOWN.containsKey(name))
                .toList(),
            "Klasa testu ma asercję bez komunikatu; dodaj komunikat z opisem oczekiwania"
        );
    }

    @Test
    void exceptionListHoldsOnlyExistingViolations() {
        final Set<String> found = AssertionMessageArchitectureTest.violators(
            AssertionMessageArchitectureTest.tests()
        );
        Assertions.assertEquals(
            List.of(),
            AssertionMessageArchitectureTest.KNOWN.keySet().stream()
                .filter(name -> !found.contains(name))
                .sorted()
                .toList(),
            "Wyjątek z listy już nie jest naruszeniem; usuń wpis"
        );
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "org.junit.jupiter.api.Assertions.assertEquals(1, 2);",
            "org.junit.jupiter.api.Assertions.assertEquals(\"a\", \"b\");",
            "org.junit.jupiter.api.Assertions.assertTrue(true);",
            "org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> { });",
            "org.hamcrest.MatcherAssert.assertThat(1, org.hamcrest.Matchers.is(1));"
        }
    )
    void rejectsAssertionWithoutMessage(final String statement, @TempDir final Path dir)
        throws IOException {
        Assertions.assertEquals(
            Set.of(String.format("%s.Sample", AssertionMessageArchitectureTest.SAMPLES)),
            AssertionMessageArchitectureTest.violators(
                AssertionMessageArchitectureTest.compiled(dir, statement)
            ),
            "Reguła przepuściła asercję bez komunikatu"
        );
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "org.junit.jupiter.api.Assertions.assertEquals(1, 2, \"liczby\");",
            "org.junit.jupiter.api.Assertions.assertEquals(\"a\", \"b\", \"napisy\");",
            "org.junit.jupiter.api.Assertions.assertTrue(true, () -> \"warunek\");",
            "org.junit.jupiter.api.Assertions.assertAll(() -> { });",
            "org.hamcrest.MatcherAssert.assertThat(\"powód\", 1, org.hamcrest.Matchers.is(1));",
            "org.hamcrest.MatcherAssert.assertThat(\"powód\", true);"
        }
    )
    void acceptsAssertionWithMessage(final String statement, @TempDir final Path dir)
        throws IOException {
        Assertions.assertEquals(
            Set.of(),
            AssertionMessageArchitectureTest.violators(
                AssertionMessageArchitectureTest.compiled(dir, statement)
            ),
            "Reguła odrzuciła asercję z komunikatem albo grupowanie assertAll"
        );
    }

    private static Set<String> violators(final JavaClasses classes) {
        return classes.stream()
            .filter(AssertionMessageArchitectureTest::quiet)
            .map(type -> type.getName().replaceAll("\\$.*", ""))
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private static boolean quiet(final JavaClass type) {
        return type.getMethodCallsFromSelf().stream()
            .anyMatch(AssertionMessageArchitectureTest::silent);
    }

    private static boolean silent(final JavaMethodCall call) {
        final String owner = call.getTargetOwner().getName();
        final String name = call.getTarget().getName();
        final List<JavaClass> params = call.getTarget().getRawParameterTypes();
        final boolean result;
        if (!name.startsWith("assert") || "assertAll".equals(name) || params.isEmpty()) {
            result = false;
        } else if ("org.junit.jupiter.api.Assertions".equals(owner)) {
            final JavaClass last = params.get(params.size() - 1);
            result = !last.isEquivalentTo(String.class)
                && !last.isEquivalentTo(Supplier.class);
        } else if ("org.hamcrest.MatcherAssert".equals(owner)) {
            result = !params.get(0).isEquivalentTo(String.class);
        } else {
            result = false;
        }
        return result;
    }

    private static JavaClasses tests() {
        return new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.ONLY_INCLUDE_TESTS)
            .importPackages("io.github.dostepnykampus");
    }

    private static JavaClasses compiled(final Path dir, final String statement)
        throws IOException {
        final ArchSamples samples = new ArchSamples(dir);
        samples.write(
            AssertionMessageArchitectureTest.SAMPLES, "Sample",
            String.format(
                "package %s;%nfinal class Sample { void check() { %s } }%n",
                AssertionMessageArchitectureTest.SAMPLES, statement
            )
        );
        return samples.compiled(AssertionMessageArchitectureTest.SAMPLES);
    }
}
