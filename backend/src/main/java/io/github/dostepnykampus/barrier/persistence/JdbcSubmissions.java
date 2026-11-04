/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.persistence;

import io.github.dostepnykampus.barrier.domain.CreateResult;
import io.github.dostepnykampus.barrier.domain.EditResult;
import io.github.dostepnykampus.barrier.domain.NewSubmission;
import io.github.dostepnykampus.barrier.domain.Submission;
import io.github.dostepnykampus.barrier.domain.SubmissionEdit;
import io.github.dostepnykampus.barrier.domain.SubmissionFinder;
import io.github.dostepnykampus.barrier.domain.SubmissionQuery;
import io.github.dostepnykampus.barrier.domain.SubmissionRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adapter portów zgłoszeń na tabeli submissions; na razie zaślepka.
 *
 * @since 0.1.0
 */
@Component
public final class JdbcSubmissions implements SubmissionRepository, SubmissionFinder {

    /**
     * Tworzy adapter.
     */
    public JdbcSubmissions() {
    }

    @Override
    public CreateResult create(final NewSubmission submission) {
        throw new UnsupportedOperationException("Zapis zgłoszeń czeka na adapter");
    }

    @Override
    public EditResult update(final SubmissionEdit edit) {
        throw new UnsupportedOperationException("Edycja zgłoszeń czeka na adapter");
    }

    @Override
    public Optional<Submission> find(final UUID id) {
        throw new UnsupportedOperationException("Odczyt zgłoszeń czeka na adapter");
    }

    @Override
    public List<Submission> list(final SubmissionQuery query) {
        throw new UnsupportedOperationException("Lista zgłoszeń czeka na adapter");
    }
}
