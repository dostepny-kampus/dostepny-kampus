/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submission.conflict

import io.github.dostepnykampus.core.model.submission.Submission
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionField

/**
 * Dwie wersje jednego zgłoszenia po odmowie 409: zapisana w telefonie i z serwera.
 *
 * Wymagania: US9.3, BR-10
 *
 * @property id identyfikator zgłoszenia
 * @property own treść zapisana w telefonie
 * @property server zgłoszenie w wersji z serwera
 * @property differences pola, którymi wersje się różnią, w kolejności [SubmissionField]
 */
public data class VersionConflict(
    val id: String,
    val own: SubmissionContent,
    val server: Submission,
    val differences: List<SubmissionField>,
)
