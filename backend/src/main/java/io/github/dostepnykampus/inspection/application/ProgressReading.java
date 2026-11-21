/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.inspection.application;

import io.github.dostepnykampus.inspection.domain.BuildingProgress;
import io.github.dostepnykampus.inspection.domain.ProgressRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bieżący postęp budynków ze znacznikami i licznikami z jednego stanu bazy.
 *
 * <p>Wymagania: US11.1, US11.2, US11.3, US11.4, BR-21, BR-24</p>
 *
 * @since 0.1.0
 */
@Service
public class ProgressReading {

    /** Dane postępu budynków. */
    private final ProgressRepository progress;

    /**
     * Tworzy usługę odczytu postępu.
     *
     * @param progress Dane postępu budynków
     */
    public ProgressReading(final ProgressRepository progress) {
        this.progress = progress;
    }

    /**
     * Odczytuje postęp każdego budynku w kolejności symboli.
     *
     * @param viewer Identyfikator konta czytającego postęp
     * @return Lista postępu z aktywnymi znacznikami
     */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<BuildingProgress> read(final String viewer) {
        return this.progress.read(viewer);
    }
}
