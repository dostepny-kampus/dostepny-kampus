/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.api;

import io.github.dostepnykampus.shared.json.RawJson;
import java.time.Instant;

/**
 * Zgłoszenie zmienione od kursora: chwila zmiany i zgłoszenie w kształcie schematu Submission.
 *
 * <p>Wymagania: US8.1, US1.3, US10.6</p>
 *
 * @param changed Chwila ostatniej zmiany zgłoszenia na serwerze
 * @param submission Zgłoszenie w kształcie schematu Submission kontraktu
 * @since 0.1.0
 */
public record ChangedSubmission(Instant changed, RawJson submission) {
}
