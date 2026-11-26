/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission;

import java.time.Instant;

/**
 * Zgłoszenie z chwilą ostatniej zmiany na serwerze.
 *
 * <p>Wymagania: US8.1, US1.3</p>
 *
 * @param submission Zgłoszenie
 * @param changed Chwila ostatniej zmiany
 * @since 0.1.0
 */
public record SubmissionChange(Submission submission, Instant changed) {
}
