/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.barrier.domain.CreateResult;
import io.github.dostepnykampus.barrier.domain.EditResult;
import io.github.dostepnykampus.barrier.domain.NewSubmission;
import io.github.dostepnykampus.barrier.domain.SubmissionEdit;
import io.github.dostepnykampus.barrier.domain.SubmissionRepository;
import java.util.ArrayList;
import java.util.List;

/**
 * Atrapa portu zapisu: zapamiętuje przyjęte komendy i zwraca podane wyniki.
 *
 * @since 0.1.0
 */
final class RecordingSubmissions implements SubmissionRepository {

    /**
     * Wynik zwracany z utworzenia.
     */
    private final CreateResult created;

    /**
     * Wynik zwracany z edycji.
     */
    private final EditResult edited;

    /**
     * Przyjęte komendy utworzenia.
     */
    private final List<NewSubmission> creations = new ArrayList<>(1);

    /**
     * Przyjęte komendy edycji.
     */
    private final List<SubmissionEdit> edits = new ArrayList<>(1);

    RecordingSubmissions(final CreateResult created, final EditResult edited) {
        this.created = created;
        this.edited = edited;
    }

    @Override
    public CreateResult create(final NewSubmission submission) {
        this.creations.add(submission);
        return this.created;
    }

    @Override
    public EditResult update(final SubmissionEdit edit) {
        this.edits.add(edit);
        return this.edited;
    }

    /**
     * Komendy utworzenia przyjęte do tej pory.
     *
     * @return Kopia listy komend
     */
    List<NewSubmission> creations() {
        return List.copyOf(this.creations);
    }

    /**
     * Komendy edycji przyjęte do tej pory.
     *
     * @return Kopia listy komend
     */
    List<SubmissionEdit> edits() {
        return List.copyOf(this.edits);
    }
}
