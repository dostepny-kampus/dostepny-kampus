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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Strażnik rozmiaru pakietów kodu produkcyjnego serwera.
 *
 * <p>Pakiet ma najwyżej {@value #LIMIT} klas najwyższego poziomu; klasy
 * zagnieżdżone i {@code package-info} się nie liczą. Pakiety ponad limitem są
 * na liście wyjątków z numerem issue. Lista może tylko maleć: wpis pakietu,
 * który mieści się w limicie, czerwieni test.</p>
 *
 * @since 0.1.0
 */
@AnalyzeClasses(
    packagesOf = DostepnyKampusApplication.class,
    importOptions = ImportOption.DoNotIncludeTests.class
)
final class PackageSizeArchitectureIT {

    /**
     * Największa dozwolona liczba klas najwyższego poziomu w pakiecie.
     */
    private static final int LIMIT = 15;

    /**
     * Pakiety ponad limitem i issue, które je dzieli.
     */
    private static final Map<String, Integer> OVERSIZED = Map.of();

    /**
     * Pakiet przykładów kompilowanych przez test.
     */
    private static final String SAMPLES = "io.github.dostepnykampus.archsamples.size";

    @ArchTest
    void packagesFitLimitOutsideExceptionList(final JavaClasses classes) {
        final Map<String, Long> found = PackageSizeArchitectureIT.oversized(classes);
        found.keySet().removeAll(PackageSizeArchitectureIT.OVERSIZED.keySet());
        Assertions.assertEquals(
            Map.of(), found,
            String.format(
                "Pakiet ma więcej niż %d klas; podziel go według tematu",
                PackageSizeArchitectureIT.LIMIT
            )
        );
    }

    @ArchTest
    void exceptionListHoldsOnlyOversizedPackages(final JavaClasses classes) {
        final List<String> stale = new ArrayList<>(
            PackageSizeArchitectureIT.OVERSIZED.keySet()
        );
        stale.removeAll(PackageSizeArchitectureIT.oversized(classes).keySet());
        Assertions.assertEquals(
            List.of(), stale,
            "Pakiet z listy wyjątków mieści się w limicie; usuń wpis"
        );
    }

    @Test
    void rejectsPackageWithSixteenClasses(@TempDir final Path dir) throws IOException {
        Assertions.assertEquals(
            Map.of(PackageSizeArchitectureIT.SAMPLES, 16L),
            PackageSizeArchitectureIT.oversized(
                PackageSizeArchitectureIT.compiled(dir, 16)
            ),
            "Reguła przepuściła pakiet z szesnastoma klasami"
        );
    }

    @Test
    void acceptsFifteenClassesWithNestedClassesAndDescription(@TempDir final Path dir)
        throws IOException {
        Assertions.assertEquals(
            Map.of(),
            PackageSizeArchitectureIT.oversized(
                PackageSizeArchitectureIT.compiled(dir, 15)
            ),
            "Reguła policzyła klasy zagnieżdżone albo package-info"
        );
    }

    private static Map<String, Long> oversized(final JavaClasses classes) {
        return classes.stream()
            .filter(JavaClass::isTopLevelClass)
            .filter(type -> !"package-info".equals(type.getSimpleName()))
            .collect(Collectors.groupingBy(JavaClass::getPackageName, Collectors.counting()))
            .entrySet().stream()
            .filter(entry -> entry.getValue() > PackageSizeArchitectureIT.LIMIT)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static JavaClasses compiled(final Path dir, final int count) throws IOException {
        final ArchSamples samples = new ArchSamples(dir);
        final String pack = PackageSizeArchitectureIT.SAMPLES;
        samples.write(pack, "package-info", String.format("package %s;%n", pack));
        for (final int index : IntStream.rangeClosed(1, count).toArray()) {
            final String name = String.format("Sample%d", index);
            samples.write(
                pack, name,
                String.format(
                    "package %s;%nfinal class %s { static final class Inner { } }%n", pack, name
                )
            );
        }
        return samples.compiled(pack, "-Xpkginfo:always");
    }
}
