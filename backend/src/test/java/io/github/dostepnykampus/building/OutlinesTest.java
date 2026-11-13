/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building;

import io.github.dostepnykampus.IntegrationTest;
import java.util.Optional;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

/**
 * Budynek zgłoszenia szybkiego to budynek, w którego konturze
 * leży punkt, a punkt tuż obok konturu jest „poza budynkiem”.
 *
 * <p>Wymagania: BR-14</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
final class OutlinesTest {

    @Test
    void brFourteenFindsBuildingOfPoint(final ApplicationContext context) {
        MatcherAssert.assertThat(
            "Punkt w konturze A-1 nie należy do A-1",
            context.getBean(Outlines.class).containing(51.107_85, 17.062_09),
            Matchers.equalTo(Optional.of("A-1"))
        );
    }

    @Test
    void brFourteenLeavesPointNearOutlineOutside(final ApplicationContext context) {
        MatcherAssert.assertThat(
            "Punkt 11 m na północ od konturu A-1 należy do budynku",
            context.getBean(Outlines.class).containing(51.108_268, 17.061_498),
            Matchers.equalTo(Optional.empty())
        );
    }
}
