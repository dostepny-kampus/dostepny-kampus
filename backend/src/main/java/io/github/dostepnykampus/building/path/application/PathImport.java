/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.path.application;

import io.github.dostepnykampus.building.application.CampusMapSettings;
import io.github.dostepnykampus.building.application.PwrImportStep;
import io.github.dostepnykampus.building.path.AccessiblePath;
import io.github.dostepnykampus.building.path.PathSource;
import io.github.dostepnykampus.building.path.PathStore;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Drugi krok importu PWr (ADR-0012): ścieżki dostosowane z warstwy mapy kampusu zastępują
 * zapisane odcinki. Błąd usługi przerywa import przed zapisem, więc zostają odcinki
 * z poprzedniego importu.
 *
 * <p>Wymagania: US1.4, NFR-40, NFR-47</p>
 *
 * @since 0.1.0
 */
@Component
@Profile("import-pwr")
@Order(2)
final class PathImport implements PwrImportStep {

    /**
     * Dziennik importu.
     */
    private static final Logger LOG = LoggerFactory.getLogger(PathImport.class);

    /**
     * Usługa mapy kampusu z warstwą ścieżek.
     */
    private final CampusMapSettings campus;

    /**
     * Usługa z odcinkami.
     */
    private final PathSource source;

    /**
     * Zapis odcinków.
     */
    private final PathStore store;

    PathImport(final CampusMapSettings campus, final PathSource source, final PathStore store) {
        this.campus = campus;
        this.source = source;
        this.store = store;
    }

    @Override
    public void run(final ApplicationArguments args) {
        final List<AccessiblePath> paths = this.source.paths(
            this.campus.service().toString(), this.campus.layers().paths()
        );
        this.store.replace(paths);
        PathImport.LOG.info("Ścieżki dostosowane: {} odcinków z usługi", paths.size());
    }
}
