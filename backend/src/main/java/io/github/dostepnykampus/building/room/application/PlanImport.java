/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room.application;

import io.github.dostepnykampus.building.room.BuildingPlan;
import io.github.dostepnykampus.building.room.FloorPlans;
import io.github.dostepnykampus.building.room.PlanFloor;
import io.github.dostepnykampus.building.room.PlanRoom;
import io.github.dostepnykampus.building.room.PlanSource;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Ręczny import planów pięter PWr (ADR-0012), uruchamiany profilem import-pwr. Każdy budynek z
 * konfiguracji dostaje piętra swojego FACILITY_ID oraz pomieszczenia i wejścia swoich warstw
 * z dniem edycji warstwy wejść.
 * Import rusza, gdy aplikacja jest gotowa (po wszystkich zadaniach startu), a po nim aplikacja
 * się zamyka. Opcja --budynek ogranicza import do podanych symboli. Każdy budynek zapisuje się
 * osobno, więc błąd usługi przerywa import, a budynki zapisane wcześniej zostają.
 *
 * <p>Wymagania: US16, US18.2, NFR-40, NFR-47, BR-24</p>
 *
 * @since 0.1.0
 */
@Component
@Profile("import-pwr")
final class PlanImport implements ApplicationListener<ApplicationReadyEvent> {

    /**
     * Dziennik importu.
     */
    private static final Logger LOG = LoggerFactory.getLogger(PlanImport.class);

    /**
     * Warstwy budynków szczegółowych.
     */
    private final PlanSettings settings;

    /**
     * Usługa z planami.
     */
    private final PlanSource source;

    /**
     * Zapis planów.
     */
    private final FloorPlans store;

    PlanImport(final PlanSettings settings, final PlanSource source, final FloorPlans store) {
        this.settings = settings;
        this.source = source;
        this.store = store;
    }

    @Override
    public void onApplicationEvent(final ApplicationReadyEvent event) {
        final List<String> only = Optional.ofNullable(
            new DefaultApplicationArguments(event.getArgs()).getOptionValues("budynek")
        ).orElse(List.of());
        this.settings.buildings().stream()
            .filter(layer -> only.isEmpty() || only.contains(layer.symbol()))
            .forEach(this::building);
        SpringApplication.exit(event.getApplicationContext());
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
                layer.symbol(), floors, rooms, this.source.entrances(service, layer.entrances()),
                this.source.edited(service, layer.entrances()).orElse(null)
            )
        );
        PlanImport.LOG.info(
            "Plan {}: {} pięter, {} pomieszczeń z usługi", layer.symbol(), floors.size(),
            rooms.size()
        );
    }
}
