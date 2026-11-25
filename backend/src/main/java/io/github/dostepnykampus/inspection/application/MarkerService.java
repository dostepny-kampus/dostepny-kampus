/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.inspection.application;

import io.github.dostepnykampus.building.api.KnownBuildings;
import io.github.dostepnykampus.inspection.domain.MarkerPlace;
import io.github.dostepnykampus.inspection.domain.MarkerRepository;
import io.github.dostepnykampus.inspection.domain.NewMarker;
import io.github.dostepnykampus.inspection.domain.SyncedMarker;
import io.github.dostepnykampus.shared.error.ForbiddenChange;
import io.github.dostepnykampus.shared.error.MissingResource;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Zapis i cofanie znaczników piętra obejrzanego: znacznik w budynku, którego serwer nie zna,
 * albo na piętrze spoza budynku jest pomijany, a pozostałe z paczki zapisują się w jednej
 * transakcji.
 *
 * <p>Wymagania: US15.1, US15.2, US15.3, BR-9, BR-20, BR-21</p>
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
     * Budynki i piętra znane serwerowi do kontroli miejsca znacznika.
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
     * @return Identyfikatory znaczników pominiętych z powodu nieznanego budynku albo piętra
     */
    @Transactional
    public Set<UUID> place(final List<NewMarker> markers) {
        final Set<UUID> unknown = new HashSet<>(0);
        for (final NewMarker marker : markers) {
            if (this.exists(marker.place())) {
                this.repository.place(marker);
            } else {
                unknown.add(marker.id());
            }
        }
        return unknown;
    }

    /**
     * Cofa znaczniki z paczki w jednej transakcji; cudzy albo nieistniejący znacznik zostaje bez
     * zmian, a cofnięcie już cofniętego jest udane.
     *
     * @param ids Identyfikatory cofanych znaczników w kolejności zapisu w telefonie
     * @param author Identyfikator konta z tokenu
     * @return Identyfikatory znaczników, których konto nie ma
     */
    @Transactional
    public Set<UUID> revoke(final List<UUID> ids, final String author) {
        final Set<UUID> missing = new HashSet<>(0);
        for (final UUID id : ids) {
            if (!this.repository.revoke(id, author)) {
                missing.add(id);
            }
        }
        return missing;
    }

    /**
     * Cofa pojedynczy znacznik: Koordynator cofa znacznik dowolnego Zbierającego, a Zbierający
     * wyłącznie własny.
     *
     * <p>Wymagania: US11.3, US15.2, BR-20</p>
     *
     * @param id Identyfikator cofanego znacznika
     * @param actor Identyfikator konta z tokenu
     * @param coordinator Prawda, gdy konto ma rolę Koordynatora
     */
    @Transactional
    public void revokeOne(final UUID id, final String actor, final boolean coordinator) {
        if (coordinator) {
            if (!this.repository.revokeAny(id)) {
                throw new MissingResource("Nie ma znacznika o tym identyfikatorze");
            }
        } else {
            final String owner = this.repository.authorOf(id).orElseThrow(
                () -> new MissingResource("Nie ma znacznika o tym identyfikatorze")
            );
            if (!owner.equals(actor)) {
                throw new ForbiddenChange("Zbierający może cofnąć wyłącznie własny znacznik");
            }
            this.repository.revoke(id, actor);
        }
    }

    /**
     * Znaczniki wszystkich Zbierających zmienione od {@code since} włącznie, także cofnięte.
     *
     * <p>Wymagania: US15.1, US15.2, US11.3</p>
     *
     * @param since Chwila, od której liczą się zmiany
     * @param reader Identyfikator konta z tokenu
     * @return Znaczniki z oznaczeniem własnych
     */
    @Transactional(readOnly = true)
    public List<SyncedMarker> changedSince(final Instant since, final String reader) {
        return this.repository.changedSince(since, reader);
    }

    private boolean exists(final MarkerPlace place) {
        final boolean exists;
        if (place instanceof MarkerPlace.Floor floor) {
            exists = this.buildings.floorLabel(floor.building(), floor.floor()).isPresent();
        } else {
            exists = this.buildings.known(place.building());
        }
        return exists;
    }
}
