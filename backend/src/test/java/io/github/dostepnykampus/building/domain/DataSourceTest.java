/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.domain;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Nazwa źródła PWr w podpisie odpowiedzi jak w kontrakcie.
 *
 * <p>Wymagania: NFR-40</p>
 *
 * @since 0.1.0
 */
final class DataSourceTest {

    @Test
    void namesPwrAsInContract() {
        MatcherAssert.assertThat(
            "Nazwa źródła PWr różni się od wartości PwrSource w kontrakcie",
            DataSource.PWR.label(),
            Matchers.equalTo("Politechnika Wrocławska")
        );
    }
}
