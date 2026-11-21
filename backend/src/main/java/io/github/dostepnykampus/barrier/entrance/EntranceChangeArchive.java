/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.entrance;

import io.github.dostepnykampus.barrier.submission.Submission;
import java.util.UUID;

/**
 * Archiwum zastosowanych zmian oznaczenia wejść.
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface EntranceChangeArchive {

    /**
     * Archiwizuje przyjętą zmianę po zastosowaniu oznaczenia.
     *
     * @param id Identyfikator zmiany
     * @return Zgłoszenie archiwalne z datą przyjęcia
     */
    Submission archiveEntranceChange(UUID id);
}
