/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.path.application;

import io.github.dostepnykampus.building.application.CampusMapSettings;
import io.github.dostepnykampus.building.path.AccessiblePath;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

/**
 * Krok importu ścieżek czyta warstwę ścieżek z usługi mapy kampusu z ustawień i zastępuje nią
 * zapisane odcinki.
 *
 * <p>Wymagania: US1.4, NFR-40, NFR-47</p>
 *
 * @since 0.1.0
 */
final class PathImportTest {

    @Test
    void replacesPathsWithLayerFromCampusMapSettings() {
        final List<String> asked = new ArrayList<>(1);
        final List<List<AccessiblePath>> saved = new ArrayList<>(1);
        final List<AccessiblePath> paths = List.of(new AccessiblePath("a", "{}"));
        new PathImport(
            new CampusMapSettings(
                URI.create("https://kampus.test/Kampus/FeatureServer"),
                new CampusMapSettings.Layers(29, 2)
            ),
            (service, layer) -> {
                asked.add(String.format("%s/%d", service, layer));
                return paths;
            },
            saved::add
        ).run(new DefaultApplicationArguments());
        MatcherAssert.assertThat(
            "Import nie przeczytał warstwy ścieżek z ustawień albo nie zastąpił nią odcinków",
            List.of(asked, saved),
            Matchers.contains(
                List.of("https://kampus.test/Kampus/FeatureServer/2"), List.of(paths)
            )
        );
    }
}
