/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room.application;

import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.building.api.KnownBuildings;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

/**
 * Konfiguracja warstw planów PWr zgadza się z tabelą buildings.
 *
 * <p>Wymagania: NFR-47, BR-G1</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
final class PlanSettingsTest {

    @Test
    void nfrFortySevenEverySymbolIsKnownBuilding(final ApplicationContext context) {
        final KnownBuildings known = context.getBean(KnownBuildings.class);
        MatcherAssert.assertThat(
            "Konfiguracja planów ma symbol spoza tabeli buildings",
            context.getBean(PlanSettings.class).buildings().stream()
                .map(PlanSettings.Layer::symbol)
                .filter(symbol -> !known.known(symbol))
                .toList(),
            Matchers.empty()
        );
    }

    @Test
    void nfrFortySevenNoSymbolOrLayerRepeats(final ApplicationContext context) {
        final List<PlanSettings.Layer> layers = context.getBean(PlanSettings.class).buildings();
        final Set<String> symbols = new HashSet<>();
        final Set<String> rooms = new HashSet<>();
        MatcherAssert.assertThat(
            "Symbol budynku albo warstwa pomieszczeń powtarza się w konfiguracji planów",
            layers.stream().filter(
                layer -> !symbols.add(layer.symbol())
                    || !rooms.add(String.format("%s/%d", layer.name(), layer.rooms()))
            ).map(PlanSettings.Layer::symbol).toList(),
            Matchers.empty()
        );
    }

    @Test
    void nfrFortySevenReadsAllDetailedBuildings(final ApplicationContext context) {
        MatcherAssert.assertThat(
            "Konfiguracja planów nie ma 24 budynków szczegółowych",
            context.getBean(PlanSettings.class).buildings(),
            Matchers.hasSize(24)
        );
    }
}
