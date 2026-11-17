/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.application;

import io.github.dostepnykampus.barrier.submission.retention.SubmissionRetention;
import io.github.dostepnykampus.barrier.submission.retention.SubmissionsPurged;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Codziennie o 3:30 usuwa zgłoszenia odrzucone ponad 90 dni temu i ogłasza zdarzenie
 * {@link SubmissionsPurged}, po którym znikają ich zdjęcia.
 *
 * <p>Wymagania: NFR-29</p>
 *
 * @since 0.1.0
 */
@Service
@EnableScheduling
public class RejectedRetention {

    /**
     * Usuwanie zgłoszeń w bazie.
     */
    private final SubmissionRetention retention;

    /**
     * Zdarzenie usunięcia dla sprzątania zdjęć po zatwierdzeniu.
     */
    private final ApplicationEventPublisher events;

    RejectedRetention(
        final SubmissionRetention retention, final ApplicationEventPublisher events
    ) {
        this.retention = retention;
        this.events = events;
    }

    /**
     * Usuwa zgłoszenia odrzucone ponad 90 dni temu.
     *
     * @return Identyfikatory usuniętych zgłoszeń
     */
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public List<UUID> purge() {
        final List<UUID> purged = this.retention.purgeRejected(90);
        this.events.publishEvent(new SubmissionsPurged(purged));
        return purged;
    }
}
