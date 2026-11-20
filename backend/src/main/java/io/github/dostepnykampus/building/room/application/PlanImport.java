/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room.application;

import io.github.dostepnykampus.building.application.CampusMapSettings;
import io.github.dostepnykampus.building.application.PwrImportStep;
import io.github.dostepnykampus.building.room.BuildingPlan;
import io.github.dostepnykampus.building.room.FloorPlans;
import io.github.dostepnykampus.building.room.PlanFloor;
import io.github.dostepnykampus.building.room.PlanRoom;
import io.github.dostepnykampus.building.room.PlanSource;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Pierwszy krok importu PWr (ADR-0012): plany pięter. Każdy budynek z konfiguracji dostaje
 * piętra swojego FACILITY_ID oraz pomieszczenia i wejścia swoich warstw z dniem edycji warstwy
 * wejść; uwagi i szerokość drzwi wejść uzupełnia warstwa mapy kampusu. Opcja --budynek ogranicza
 * import do podanych symboli. Każdy budynek zapisuje się osobno, więc błąd usługi przerywa
 * import, a budynki zapisane wcześniej zostają.
 *
 * <p>Wymagania: US16, US18.2, NFR-40, NFR-47, BR-24, BR-27</p>
 *
 * @since 0.1.0
 */
@Component
@Profile("import-pwr")
@Order(1)
final class PlanImport implements PwrImportStep {

    /**
     * Dziennik importu.
     */
    private static final Logger LOG = LoggerFactory.getLogger(PlanImport.class);

    /**
     * Warstwy budynków szczegółowych.
     */
    private final PlanSettings settings;

    /**
     * Usługa mapy kampusu z warstwą wejść.
     */
    private final CampusMapSettings campus;

    /**
     * Usługa z planami.
     */
    private final PlanSource source;

    /**
     * Zapis planów.
     */
    private final FloorPlans store;

    PlanImport(
        final PlanSettings settings, final CampusMapSettings campus, final PlanSource source,
        final FloorPlans store
    ) {
        this.settings = settings;
        this.campus = campus;
        this.source = source;
        this.store = store;
    }

    @Override
    public void run(final ApplicationArguments args) {
        final List<String> only = Optional.ofNullable(args.getOptionValues("budynek"))
            .orElse(List.of());
        this.settings.buildings().stream()
            .filter(layer -> only.isEmpty() || only.contains(layer.symbol()))
            .forEach(this::building);
    }

    private void building(final PlanSettings.Layer layer) {
        final String service = String.format(
            "%s/%s/FeatureServer", this.settings.service(), layer.name()
        );
        final String prefix = String.format("%s.", layer.facility());
        final List<PlanFloor> floors = this.source.floors(service, layer.levels()).stream()
            .filter(floor -> floor.id().startsWith(prefix)).toList();
        final List<PlanRoom> rooms = this.source.rooms(service, layer.rooms());
        this.store.save(
            new BuildingPlan(
                layer.symbol(), floors, rooms, this.source.entrances(
                    new PlanSource.Layer(service, layer.entrances()),
                    new PlanSource.Layer(
                        this.campus.service().toString(), this.campus.layers().entrances()
                    )
                ),
                this.source.edited(service, layer.entrances()).orElse(null)
            )
        );
        PlanImport.LOG.info(
            "Plan {}: {} pięter, {} pomieszczeń z usługi", layer.symbol(), floors.size(),
            rooms.size()
        );
    }
}
