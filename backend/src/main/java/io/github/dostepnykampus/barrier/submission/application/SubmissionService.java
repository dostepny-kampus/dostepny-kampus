/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.application;

import io.github.dostepnykampus.barrier.submission.CreateResult;
import io.github.dostepnykampus.barrier.submission.EditResult;
import io.github.dostepnykampus.barrier.submission.NewSubmission;
import io.github.dostepnykampus.barrier.submission.OutsideCampus;
import io.github.dostepnykampus.barrier.submission.SubmissionEdit;
import io.github.dostepnykampus.barrier.submission.SubmissionRepository;
import io.github.dostepnykampus.barrier.submission.content.InvalidSubmission;
import io.github.dostepnykampus.barrier.submission.content.SubmissionContent;
import io.github.dostepnykampus.barrier.submission.content.SubmissionPoint;
import io.github.dostepnykampus.building.api.CampusArea;
import io.github.dostepnykampus.building.api.KnownBuildings;
import io.github.dostepnykampus.building.api.Outlines;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Zapis zgłoszeń: budynek zgłoszenia szybkiego ustala serwer z konturu,
 * a nowe zgłoszenie spoza terenu PWr odrzuca.
 *
 * <p>Wymagania: BR-14, BR-G2, BR-G5, US4.9</p>
 *
 * @since 0.1.0
 */
@Service
public class SubmissionService {

    /** Zapis zgłoszeń. */
    private final SubmissionRepository repository;

    /** Kontury budynków do budynku zgłoszenia szybkiego. Wymagania: BR-14 */
    private final Outlines outlines;

    /** Budynki znane serwerowi do kontroli budynku podanego przez klienta. Wymagania: BR-14 */
    private final KnownBuildings buildings;

    /** Teren PWr, poza którym nowe zgłoszenie jest odrzucane. Wymagania: BR-G5 */
    private final CampusArea area;

    /** Zdarzenie zapisanej edycji dla sprzątania zdjęć po zatwierdzeniu. */
    private final ApplicationEventPublisher events;

    SubmissionService(
        final SubmissionRepository repository, final Outlines outlines,
        final KnownBuildings buildings, final CampusArea area,
        final ApplicationEventPublisher events
    ) {
        this.repository = repository;
        this.outlines = outlines;
        this.buildings = buildings;
        this.area = area;
        this.events = events;
    }

    /**
     * Wykonuje operację z blokadą istniejącego zgłoszenia do końca transakcji.
     * Brak zgłoszenia obsługuje operacja; błąd operacji zwalnia blokadę.
     *
     * @param id Identyfikator zgłoszenia
     * @param write Kontrola statusu i zapis zdjęcia
     */
    @Transactional
    public void locked(final UUID id, final Runnable write) {
        this.repository.lock(id);
        write.run();
    }

    /**
     * Zapisuje nowe zgłoszenie albo zwraca zapisane wcześniej.
     * Miejsce poza granicą każdego terenu PWr daje {@link OutsideCampus};
     * przynależność do terenu rozstrzyga baza. Nieznany budynek podany przez klienta daje
     * {@link InvalidSubmission} bez zapisu.
     *
     * <p>Wymagania: BR-9, BR-14, BR-G5</p>
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
            new NewSubmission(
                command.id(), this.placed(command.content()), command.author(), command.roles()
            )
        );
    }

    /**
     * Zapisuje edycję zgłoszenia. Zapisana edycja idzie jako
     * zdarzenie, więc po zatwierdzeniu zdjęcia spoza nowej listy znikają z
     * magazynu. Budynek sprawdza się jak przy utworzeniu.
     *
     * <p>Wymagania: BR-3, BR-10, BR-14</p>
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
        if (content.kind().buildingFromOutline() && content.entranceId() == null) {
            final SubmissionPoint point = content.place().point();
            result = this.outlines.containing(point.latitude(), point.longitude())
                .map(content::withBuilding)
                .orElseGet(content::outsideBuildings);
        } else {
            final String symbol = content.place().buildingSymbol();
            if (symbol != null && !this.buildings.known(symbol)) {
                throw new InvalidSubmission("Nieznany budynek zgłoszenia");
            }
            result = content;
        }
        return result;
    }
}
