/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.inspection.web;

import io.github.dostepnykampus.inspection.application.ProgressReading;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Odczyt bieżącego postępu inwentaryzacji dla Koordynatora.
 *
 * <p>Wymagania: US11.1, US11.2, US11.3, US11.4</p>
 *
 * @since 0.1.0
 */
@RestController
public final class ProgressController {

    /** Odczyt postępu budynków. */
    private final ProgressReading progress;

    ProgressController(final ProgressReading progress) {
        this.progress = progress;
    }

    @GetMapping("/progress")
    List<BuildingProgressResponse> read(final Principal account) {
        return this.progress.read(account.getName()).stream()
            .map(BuildingProgressResponse::from).toList();
    }
}
