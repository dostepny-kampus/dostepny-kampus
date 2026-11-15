/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Strażnik wzorców klas mutowanych przez pitest w {@code pom.xml}.
 *
 * <p>Każdy wzorzec z {@code targetClasses} pasuje do co najmniej jednej
 * klasy produkcyjnej. Gwiazdka we wzorcu oznacza dowolny ciąg znaków.</p>
 *
 * @since 0.1.0
 */
@AnalyzeClasses(
    packagesOf = DostepnyKampusApplication.class,
    importOptions = ImportOption.DoNotIncludeTests.class
)
final class MutationTargetsArchitectureIT {

    /**
     * Lista wzorców klas mutowanych.
     */
    private static final Pattern TARGETS = Pattern.compile(
        "<targetClasses>(.*?)</targetClasses>", Pattern.DOTALL
    );

    /**
     * Jeden wzorzec na liście.
     */
    private static final Pattern PARAM = Pattern.compile("<param>\\s*([^<\\s]+)\\s*</param>");

    @ArchTest
    void everyTargetPatternMatchesClass(final JavaClasses classes) throws IOException {
        final List<String> patterns = MutationTargetsArchitectureIT.patterns(
            Files.readString(Path.of("pom.xml"))
        );
        Assertions.assertAll(
            () -> Assertions.assertFalse(
                patterns.isEmpty(), "Test nie znalazł targetClasses w pom.xml"
            ),
            () -> Assertions.assertEquals(
                List.of(),
                MutationTargetsArchitectureIT.unmatched(
                    patterns,
                    classes.stream().map(JavaClass::getName).collect(Collectors.toSet())
                ),
                "Wzorzec targetClasses w pom.xml nie pasuje do żadnej klasy; popraw pakiet"
            )
        );
    }

    @Test
    void rejectsPatternWithoutClass() {
        Assertions.assertEquals(
            List.of("a.Moved*"),
            MutationTargetsArchitectureIT.unmatched(
                MutationTargetsArchitectureIT.patterns(
                    String.join(
                        "", "<targetClasses>", "<param>a.*.domain.*</param>",
                        "<param>a.Moved*</param>", "</targetClasses>"
                    )
                ),
                Set.of("a.b.domain.Model", "a.shared.Moved")
            ),
            "Reguła przepuściła wzorzec bez pasującej klasy"
        );
    }

    private static List<String> patterns(final String pom) {
        return MutationTargetsArchitectureIT.TARGETS.matcher(pom).results().flatMap(
            block -> MutationTargetsArchitectureIT.PARAM.matcher(block.group(1)).results()
        ).map(param -> param.group(1)).toList();
    }

    private static List<String> unmatched(final List<String> patterns, final Set<String> names) {
        return patterns.stream().filter(
            glob -> names.stream().noneMatch(
                MutationTargetsArchitectureIT.regex(glob).asMatchPredicate()
            )
        ).toList();
    }

    private static Pattern regex(final String glob) {
        return Pattern.compile(
            Arrays.stream(glob.split("\\*", -1)).map(Pattern::quote)
                .collect(Collectors.joining(".*"))
        );
    }
}
