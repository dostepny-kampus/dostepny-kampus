/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room.application;

import io.github.dostepnykampus.building.application.CampusMapSettings;
import io.github.dostepnykampus.building.room.PlanEntrance;
import io.github.dostepnykampus.building.room.PlanFloor;
import io.github.dostepnykampus.building.room.PlanRoom;
import io.github.dostepnykampus.building.room.PlanSource;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;

/**
 * Krok importu planów pobiera warstwy budynku i warstwę wejść mapy kampusu i zapisuje plan.
 *
 * <p>Wymagania: US16, NFR-47, BR-24</p>
 *
 * @since 0.1.0
 */
final class PlanImportTest {

    /**
     * Ustawienia z dwoma budynkami na wspólnej warstwie pięter.
     */
    private static final PlanSettings SETTINGS = new PlanSettings(
        URI.create("https://plany.test/services"),
        List.of(
            new PlanSettings.Layer("D-1", "D1D2", "K1.D1", 5, 1, 0),
            new PlanSettings.Layer("D-2", "D1D2", "K1.D2", 5, 3, 2)
        )
    );

    /**
     * Usługa mapy kampusu pod innym adresem niż plany.
     */
    private static final CampusMapSettings CAMPUS_MAP = new CampusMapSettings(
        URI.create("https://kampus.test/Kampus/FeatureServer"),
        new CampusMapSettings.Layers(29, 2)
    );

    /**
     * Usługa planów obu budynków.
     */
    private static final String PLANS = "https://plany.test/services/D1D2/FeatureServer";

    /**
     * Warstwa wejść mapy kampusu z uwagami i szerokością drzwi.
     */
    private static final String CAMPUS = "https://kampus.test/Kampus/FeatureServer/29";

    /**
     * Piętra wspólnej warstwy pięter dwóch budynków.
     */
    private static final List<PlanFloor> FLOORS = List.of(
        new PlanFloor("K1.D1.P0", "Bud. D1 parter", 0),
        new PlanFloor("K1.D2.P0", "Bud. D2 parter", 0)
    );

    @Test
    void savesFloorsOfOwnFacilityAndRoomsOfOwnLayer() {
        final List<String> saved = new ArrayList<>(2);
        new PlanImport(
            PlanImportTest.SETTINGS, PlanImportTest.CAMPUS_MAP, PlanImportTest.source(),
            plan -> saved.add(
                String.format(
                    "%s %s %s %s", plan.building(),
                    plan.floors().stream().map(PlanFloor::id).toList(),
                    plan.rooms().stream().map(PlanRoom::id).toList(),
                    plan.entrances().stream().map(PlanEntrance::floorId).toList()
                )
            )
        ).run(PlanImportTest.args());
        MatcherAssert.assertThat(
            "Import zapisał piętra innego budynku albo pomieszczenia lub wejścia z cudzej warstwy",
            saved,
            Matchers.contains(
                "D-1 [K1.D1.P0] [https://plany.test/services/D1D2/FeatureServer/1] [%s/0 %s]"
                    .formatted(PlanImportTest.PLANS, PlanImportTest.CAMPUS),
                "D-2 [K1.D2.P0] [https://plany.test/services/D1D2/FeatureServer/3] [%s/2 %s]"
                    .formatted(PlanImportTest.PLANS, PlanImportTest.CAMPUS)
            )
        );
    }

    @Test
    void importsOnlyRequestedBuilding() {
        final List<String> saved = new ArrayList<>(2);
        new PlanImport(
            PlanImportTest.SETTINGS, PlanImportTest.CAMPUS_MAP, PlanImportTest.source(),
            plan -> saved.add(plan.building())
        ).run(PlanImportTest.args("--budynek=D-2"));
        MatcherAssert.assertThat(
            "Import z opcją --budynek zapisał inny budynek",
            saved,
            Matchers.contains("D-2")
        );
    }

    @Test
    void passesEditDateOfOwnEntranceLayer() {
        final List<String> saved = new ArrayList<>(2);
        new PlanImport(
            PlanImportTest.SETTINGS, PlanImportTest.CAMPUS_MAP, PlanImportTest.source(),
            plan -> saved.add(String.format("%s %s", plan.building(), plan.edited()))
        ).run(PlanImportTest.args());
        MatcherAssert.assertThat(
            "Plan nie dostał daty edycji swojej warstwy wejść",
            saved,
            Matchers.contains("D-1 2026-11-01", "D-2 2026-11-03")
        );
    }

    private static ApplicationArguments args(final String... args) {
        return new DefaultApplicationArguments(args);
    }

    private static PlanSource source() {
        return new PlanImportTest.Source();
    }

    private static PlanEntrance entrance(final String floor) {
        return new PlanEntrance(floor, "{}", "NO_DATA", "Wejście", null, null, false);
    }

    /**
     * Usługa z planami, która zwraca wpisy z adresem usługi i numerem warstwy.
     *
     * @since 0.1.0
     */
    private static final class Source implements PlanSource {

        @Override
        public List<PlanFloor> floors(final String service, final int layer) {
            return PlanImportTest.FLOORS;
        }

        @Override
        public List<PlanRoom> rooms(final String service, final int layer) {
            return List.of(
                new PlanRoom(
                    String.format("%s/%d", service, layer), "K1.D1.P0", "1", "Sala", "{}"
                )
            );
        }

        @Override
        public List<PlanEntrance> entrances(
            final PlanSource.Layer plan, final PlanSource.Layer campus
        ) {
            return List.of(
                PlanImportTest.entrance(
                    String.format(
                        "%s/%d %s/%d", plan.service(), plan.number(), campus.service(),
                        campus.number()
                    )
                )
            );
        }

        @Override
        public Optional<LocalDate> edited(final String service, final int layer) {
            return Optional.of(LocalDate.of(2026, 11, layer + 1));
        }
    }
}
