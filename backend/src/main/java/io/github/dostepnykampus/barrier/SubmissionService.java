/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.barrier.domain.CreateResult;
import io.github.dostepnykampus.barrier.domain.EditResult;
import io.github.dostepnykampus.barrier.domain.NewSubmission;
import io.github.dostepnykampus.barrier.domain.OutsideCampus;
import io.github.dostepnykampus.barrier.domain.SubmissionContent;
import io.github.dostepnykampus.barrier.domain.SubmissionEdit;
import io.github.dostepnykampus.barrier.domain.SubmissionPoint;
import io.github.dostepnykampus.barrier.domain.SubmissionRepository;
import io.github.dostepnykampus.building.CampusArea;
import io.github.dostepnykampus.building.Outlines;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Zapis zgłoszeń: budynek zgłoszenia szybkiego ustala serwer z konturu (BR-14, BR-G2),
 * a nowe zgłoszenie spoza terenu PWr odrzuca (BR-G5, US4.9).
 *
 * @since 0.1.0
 */
@Service
public class SubmissionService {

    /** Zapis zgłoszeń. */
    private final SubmissionRepository repository;

    /** Kontury budynków do budynku zgłoszenia szybkiego (BR-14). */
    private final Outlines outlines;

    /** Teren PWr, poza którym nowe zgłoszenie jest odrzucane (BR-G5). */
    private final CampusArea area;

    /** Zdarzenie zapisanej edycji dla sprzątania zdjęć po zatwierdzeniu (#839). */
    private final ApplicationEventPublisher events;

    SubmissionService(
        final SubmissionRepository repository, final Outlines outlines,
        final CampusArea area, final ApplicationEventPublisher events
    ) {
        this.repository = repository;
        this.outlines = outlines;
        this.area = area;
        this.events = events;
    }

    /**
     * Zapisuje nowe zgłoszenie albo zwraca zapisane wcześniej (BR-9).
     * Miejsce dalej niż 500 m od każdego budynku PWr daje {@link OutsideCampus};
     * odległość od budynków liczy baza (BR-G5).
     *
     * @param command Komenda utworzenia
     * @return Wynik zapisu
     */
    @Transactional
    public CreateResult create(final NewSubmission command) {
        final SubmissionPoint point = command.content().place().point();
        if (!this.area.covers(point.latitude(), point.longitude())) {
            throw new OutsideCampus();
        }
        return this.repository.create(
            new NewSubmission(command.id(), this.placed(command.content()), command.author())
        );
    }

    /**
     * Zapisuje edycję zgłoszenia (BR-3, BR-10). Zapisana edycja idzie jako
     * zdarzenie, więc po zatwierdzeniu zdjęcia spoza nowej listy znikają z
     * magazynu (#839).
     *
     * @param command Komenda edycji
     * @return Wynik edycji
     */
    @Transactional
    public EditResult update(final SubmissionEdit command) {
        final EditResult result = this.repository.update(
            new SubmissionEdit(
                command.id(), command.expectedVersion(), this.placed(command.content())
            )
        );
        if (result instanceof EditResult.Saved) {
            this.events.publishEvent(result);
        }
        return result;
    }

    private SubmissionContent placed(final SubmissionContent content) {
        final SubmissionContent result;
        if (content.kind().buildingFromOutline()) {
            final SubmissionPoint point = content.place().point();
            result = this.outlines.containing(point.latitude(), point.longitude())
                .map(content::withBuilding)
                .orElseGet(content::outsideBuildings);
        } else {
            result = content;
        }
        return result;
    }
}
