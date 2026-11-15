/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.inspection.application;

import io.github.dostepnykampus.building.api.KnownBuildings;
import io.github.dostepnykampus.inspection.domain.MarkerRepository;
import io.github.dostepnykampus.inspection.domain.NewMarker;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Zapis znaczników piętra obejrzanego: znacznik w budynku, którego serwer nie zna, jest
 * pomijany, a pozostałe z paczki zapisują się w jednej transakcji.
 *
 * <p>Wymagania: US15.1, US15.3, BR-9, BR-20</p>
 *
 * @since 0.1.0
 */
@Service
public class MarkerService {

    /**
     * Zapis znaczników.
     */
    private final MarkerRepository repository;

    /**
     * Budynki znane serwerowi do kontroli budynku znacznika.
     */
    private final KnownBuildings buildings;

    MarkerService(final MarkerRepository repository, final KnownBuildings buildings) {
        this.repository = repository;
        this.buildings = buildings;
    }

    /**
     * Zapisuje znaczniki z paczki w jednej transakcji; błąd zapisu któregokolwiek wycofuje
     * wszystkie.
     *
     * @param markers Znaczniki w kolejności zapisu w telefonie
     * @return Identyfikatory znaczników pominiętych z powodu nieznanego budynku
     */
    @Transactional
    public Set<UUID> place(final List<NewMarker> markers) {
        final Set<UUID> unknown = new HashSet<>(0);
        for (final NewMarker marker : markers) {
            if (this.buildings.known(marker.place().building())) {
                this.repository.place(marker);
            } else {
                unknown.add(marker.id());
            }
        }
        return unknown;
    }
}
