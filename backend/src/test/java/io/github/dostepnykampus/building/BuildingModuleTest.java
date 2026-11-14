/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building;

import io.github.dostepnykampus.ModuleBeans;
import io.github.dostepnykampus.ModuleTest;
import io.github.dostepnykampus.building.api.Outlines;
import java.util.Optional;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

/**
 * Moduł budynków uruchamia się bez modułów zgłoszeń i tożsamości, importuje budynki z OSM
 * i znajduje budynek punktu.
 *
 * <p>Wymagania: BR-G2</p>
 *
 * @since 0.1.0
 */
@ModuleTest
final class BuildingModuleTest {

    @Test
    void startsWithoutOtherModules(final ApplicationContext context) {
        MatcherAssert.assertThat(
            "Kontekst modułu budynków ma beany innego modułu",
            new ModuleBeans(context).in("barrier", "identity"),
            Matchers.empty()
        );
    }

    @Test
    void findsBuildingInModuleContext(final ApplicationContext context) {
        MatcherAssert.assertThat(
            "Moduł budynków bez innych modułów nie znajduje A-1 w jego konturze",
            context.getBean(Outlines.class).containing(51.107_85, 17.062_09),
            Matchers.equalTo(Optional.of("A-1"))
        );
    }
}
