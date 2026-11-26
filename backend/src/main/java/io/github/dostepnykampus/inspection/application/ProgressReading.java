/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.inspection.application;

import io.github.dostepnykampus.barrier.api.SubmissionCounts;
import io.github.dostepnykampus.building.api.BuildingInventory;
import io.github.dostepnykampus.inspection.domain.BuildingProgress;
import io.github.dostepnykampus.inspection.domain.MarkerPlace;
import io.github.dostepnykampus.inspection.domain.ProgressMarker;
import io.github.dostepnykampus.inspection.domain.ProgressRepository;
import io.github.dostepnykampus.inspection.domain.Pseudonyms;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bieżący postęp budynków ze znacznikami i licznikami z jednego stanu bazy.
 *
 * <p>Wymagania: US11.1, US11.2, US11.3, US11.4, BR-5, BR-21, BR-24</p>
 *
 * @since 0.1.0
 */
@Service
public class ProgressReading {

    /** Aktywne znaczniki. */
    private final ProgressRepository progress;

    /** Piętra i wejścia budynków. */
    private final BuildingInventory inventory;

    /** Liczniki zgłoszeń. */
    private final SubmissionCounts submissions;

    /** Pseudonimy autorów znaczników. */
    private final Pseudonyms pseudonyms;

    /**
     * Tworzy usługę odczytu postępu.
     *
     * @param progress Aktywne znaczniki
     * @param inventory Piętra i wejścia budynków
     * @param submissions Liczniki zgłoszeń
     * @param pseudonyms Pseudonimy autorów znaczników
     */
    public ProgressReading(
        final ProgressRepository progress, final BuildingInventory inventory,
        final SubmissionCounts submissions, final Pseudonyms pseudonyms
    ) {
        this.progress = progress;
        this.inventory = inventory;
        this.submissions = submissions;
        this.pseudonyms = pseudonyms;
    }

    /**
     * Odczytuje postęp każdego budynku w kolejności symboli.
     *
     * @param viewer Identyfikator konta czytającego postęp
     * @return Lista postępu z aktywnymi znacznikami
     */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<BuildingProgress> read(final String viewer) {
        final Map<String, List<ProgressRepository.ActiveMarker>> markers = this.progress.activeMarkers()
            .stream().collect(Collectors.groupingBy(marker -> marker.place().building()));
        final Map<String, SubmissionCounts.Counts> counts = this.submissions.byBuilding();
        final SubmissionCounts.Counts empty = new SubmissionCounts.Counts(0, 0);
        return this.inventory.buildings().stream().map(building -> this.building(
            building, markers.getOrDefault(building.symbol(), List.of()),
            counts.getOrDefault(building.symbol(), empty), viewer
        )).toList();
    }

    private BuildingProgress building(
        final BuildingInventory.Building building, final List<ProgressRepository.ActiveMarker> markers,
        final SubmissionCounts.Counts counts, final String viewer
    ) {
        return new BuildingProgress(
            building.symbol(), ProgressReading.coverage(building, markers),
            new BuildingProgress.Counts(
                counts.pending(), counts.accepted(), building.entrances(), building.checked()
            ),
            markers.stream().map(marker -> new ProgressMarker(
                marker.id(), marker.place(), marker.created(), viewer.equals(marker.author()),
                this.pseudonyms.of(marker.author())
            )).toList()
        );
    }

    private static BuildingProgress.Coverage coverage(
        final BuildingInventory.Building building, final List<ProgressRepository.ActiveMarker> markers
    ) {
        if (!building.detailed()) {
            return new BuildingProgress.Coverage.Whole(!markers.isEmpty());
        }
        final long covered = markers.stream().map(ProgressRepository.ActiveMarker::place)
            .filter(MarkerPlace.Floor.class::isInstance).map(MarkerPlace.Floor.class::cast)
            .map(MarkerPlace.Floor::floor).filter(building.floors()::contains).distinct().count();
        return new BuildingProgress.Coverage.Floors(building.floors().size(), Math.toIntExact(covered));
    }
}
