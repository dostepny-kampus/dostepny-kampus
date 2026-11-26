/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.application;

import io.github.dostepnykampus.IntegrationTest;
import java.net.URI;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

/**
 * Ustawienia serwera wskazują usługę mapy kampusu PWr i jej warstwy.
 *
 * <p>Wymagania: NFR-47</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
final class CampusMapSettingsIT {

    @Test
    void readsCampusMapServiceAndLayers(final ApplicationContext context) {
        MatcherAssert.assertThat(
            "Ustawienia nie wskazują usługi mapy kampusu albo jej warstwy wejść",
            context.getBean(CampusMapSettings.class),
            Matchers.equalTo(
                new CampusMapSettings(
                    URI.create(
                        String.join(
                            "/", "https://services3.arcgis.com/rrLByHvJ7zuJQDVw/arcgis/rest",
                            "services/MAPA_KAMPUSU_PWr_14122023_WFL1_widok/FeatureServer"
                        )
                    ),
                    new CampusMapSettings.Layers(29, 2)
                )
            )
        );
    }
}
