/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Zgłoszenie zapisane na serwerze, schemat Submission w OpenAPI (US8, BR-3).
 *
 * @param id Identyfikator zgłoszenia nadany przez klienta
 * @param version Wersja zgłoszenia, co najmniej 1
 * @param status Status zgłoszenia
 * @param createdAt Chwila utworzenia zgłoszenia
 * @param content Treść zgłoszenia
 * @since 0.1.0
 */
public record Submission(
    UUID id, int version, SubmissionStatus status, Instant createdAt, SubmissionContent content
) {
}
