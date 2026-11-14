/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.JavaPackage;

/**
 * Warstwy modułów serwera dla testów reguł architektury. Warstwą klasy jest pierwszy segment
 * pakietu pod korzeniem modułu, który jest nazwą warstwy; klasa bez takiego segmentu należy do
 * dziedziny ({@code domain}), a klasa w korzeniu modułu do API ({@code api}).
 *
 * @since 0.1.0
 */
final class ModuleLayers {

    /**
     * Nazwy warstw rozpoznawane w pakiecie.
     */
    private static final List<String> LAYERS = List.of(
        "api", "application", "web", "persistence", "json", "storage"
    );

    /**
     * Klasa bez instancji.
     */
    private ModuleLayers() {
    }

    /**
     * Klasy jednej warstwy w podanych modułach.
     *
     * @param roots Pakiety bazowe modułów
     * @param layer Nazwa warstwy albo {@code domain}
     * @return Predykat klas tej warstwy
     */
    static DescribedPredicate<JavaClass> in(final Collection<String> roots, final String layer) {
        return DescribedPredicate.describe(
            String.format("warstwa %s", layer),
            (JavaClass type) -> roots.stream().anyMatch(
                root -> layer.equals(ModuleLayers.layerOf(type.getPackageName(), root))
            )
        );
    }

    /**
     * Pakiety bazowe zamkniętych modułów aplikacji.
     *
     * @return Nazwy pakietów bazowych
     */
    static List<String> modules() {
        return ApplicationModules.of(DostepnyKampusApplication.class).stream()
            .filter(module -> !module.isOpen())
            .map(ApplicationModule::getBasePackage)
            .map(JavaPackage::getName)
            .toList();
    }

    private static String layerOf(final String pack, final String root) {
        String result = "";
        if (pack.equals(root)) {
            result = "api";
        } else if (pack.startsWith(String.format("%s.", root))) {
            result = Stream.of(pack.substring(root.length() + 1).split("\\.", -1))
                .filter(ModuleLayers.LAYERS::contains)
                .findFirst()
                .orElse("domain");
        }
        return result;
    }
}
