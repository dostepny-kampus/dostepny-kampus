/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.ModuleBeans;
import io.github.dostepnykampus.ModuleTest;
import io.github.dostepnykampus.RustfsTestConfiguration;
import io.github.dostepnykampus.barrier.submission.web.SubmissionsController;
import io.github.dostepnykampus.building.api.CampusArea;
import io.github.dostepnykampus.building.api.EntranceMarkings;
import io.github.dostepnykampus.building.api.KnownBuildings;
import io.github.dostepnykampus.building.api.Outlines;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Moduł zgłoszeń uruchamia się bez modułów budynków i tożsamości; z budynków potrzebuje
 * tylko typów ich API, które test zastępuje.
 *
 * @since 0.1.0
 */
@ModuleTest
@Import(RustfsTestConfiguration.class)
final class BarrierModuleIT {

    /**
     * Kontury budynków z API modułu budynków.
     */
    @MockitoBean
    private Outlines outlines;

    /**
     * Obszar kampusu z API modułu budynków.
     */
    @MockitoBean
    private CampusArea area;

    /**
     * Budynki znane serwerowi z API modułu budynków.
     */
    @MockitoBean
    private KnownBuildings buildings;

    /** Zapis oznaczenia wejścia przez API modułu budynków. */
    @MockitoBean
    private EntranceMarkings markings;

    @Test
    void usesOnlyApiOfOtherModules(final ApplicationContext context) {
        MatcherAssert.assertThat(
            "Kontekst modułu zgłoszeń ma beany innego modułu spoza jego API",
            new ModuleBeans(context).in("building", "identity").stream()
                .filter(name -> !name.startsWith("io.github.dostepnykampus.building.api."))
                .toList(),
            Matchers.empty()
        );
    }

    @Test
    void servesSubmissionsInModuleContext(final ApplicationContext context) {
        MatcherAssert.assertThat(
            "Moduł zgłoszeń bez innych modułów nie ma kontrolera zgłoszeń",
            context.getBeanNamesForType(SubmissionsController.class),
            Matchers.arrayWithSize(1)
        );
    }
}
