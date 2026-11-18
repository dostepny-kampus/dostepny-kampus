/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room.application;

import io.github.dostepnykampus.building.room.PlanEntrance;
import io.github.dostepnykampus.building.room.PlanFloor;
import io.github.dostepnykampus.building.room.PlanRoom;
import io.github.dostepnykampus.building.room.PlanSource;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.support.StaticApplicationContext;

/**
 * Polecenie importu pobiera warstwy budynku i zapisuje plan.
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
            PlanImportTest.SETTINGS, PlanImportTest.source(),
            plan -> saved.add(
                String.format(
                    "%s %s %s %s", plan.building(),
                    plan.floors().stream().map(PlanFloor::id).toList(),
                    plan.rooms().stream().map(PlanRoom::id).toList(),
                    plan.entrances().stream().map(PlanEntrance::floorId).toList()
                )
            )
        ).onApplicationEvent(PlanImportTest.ready());
        MatcherAssert.assertThat(
            "Import zapisał piętra innego budynku albo pomieszczenia lub wejścia z cudzej warstwy",
            saved,
            Matchers.contains(
                "D-1 [K1.D1.P0] [https://plany.test/services/D1D2/FeatureServer/1] [%s/0]"
                    .formatted("https://plany.test/services/D1D2/FeatureServer"),
                "D-2 [K1.D2.P0] [https://plany.test/services/D1D2/FeatureServer/3] [%s/2]"
                    .formatted("https://plany.test/services/D1D2/FeatureServer")
            )
        );
    }

    @Test
    void importsOnlyRequestedBuilding() {
        final List<String> saved = new ArrayList<>(2);
        new PlanImport(
            PlanImportTest.SETTINGS, PlanImportTest.source(),
            plan -> saved.add(plan.building())
        ).onApplicationEvent(PlanImportTest.ready("--budynek=D-2"));
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
            PlanImportTest.SETTINGS, PlanImportTest.source(),
            plan -> saved.add(String.format("%s %s", plan.building(), plan.edited()))
        ).onApplicationEvent(PlanImportTest.ready());
        MatcherAssert.assertThat(
            "Plan nie dostał daty edycji swojej warstwy wejść",
            saved,
            Matchers.contains("D-1 2026-11-01", "D-2 2026-11-03")
        );
    }

    private static ApplicationReadyEvent ready(final String... args) {
        final StaticApplicationContext context = new StaticApplicationContext();
        context.refresh();
        return new ApplicationReadyEvent(new SpringApplication(), args, context, Duration.ZERO);
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
        public List<PlanEntrance> entrances(final String service, final int layer) {
            return List.of(PlanImportTest.entrance(String.format("%s/%d", service, layer)));
        }

        @Override
        public Optional<LocalDate> edited(final String service, final int layer) {
            return Optional.of(LocalDate.of(2026, 11, layer + 1));
        }
    }
}
