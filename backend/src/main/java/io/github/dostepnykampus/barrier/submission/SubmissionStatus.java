/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission;

/**
 * Status zgłoszenia na serwerze, wartości jak schemat SubmissionStatus
 * w OpenAPI.
 *
 * <p>Wymagania: US8.1, US8.2</p>
 *
 * @since 0.1.0
 */
public enum SubmissionStatus {

    /** Wysłane, czeka na weryfikację. */
    SENT,

    /** W trakcie weryfikacji. */
    IN_REVIEW,

    /** Przyjęte przez Weryfikatora, widoczne na mapie. */
    ACCEPTED,

    /** Odrzucone przez Weryfikatora. */
    REJECTED,

    /** Zastąpione przyjętą zmianą. Wymagania: BR-18 */
    ARCHIVED;

    /**
     * Czy autor może zmienić zgłoszenie i jego zdjęcia:
     * tylko przed decyzją Weryfikatora albo po odrzuceniu.
     *
     * <p>Wymagania: BR-3, BR-18</p>
     *
     * @return Prawda dla SENT, IN_REVIEW i REJECTED
     */
    public boolean editable() {
        return this == SubmissionStatus.SENT
            || this == SubmissionStatus.IN_REVIEW
            || this == SubmissionStatus.REJECTED;
    }

    /**
     * Status po zapisaniu edycji autora: odrzucone wraca do
     * wysłanych, pozostałe edytowalne zostają; dla statusu, którego nie
     * wolno edytować, rzuca IllegalStateException.
     *
     * <p>Wymagania: BR-3</p>
     *
     * @return Status zgłoszenia po edycji
     */
    public SubmissionStatus afterEdit() {
        if (!this.editable()) {
            throw new IllegalStateException(
                String.format("Zgłoszenia w statusie %s nie wolno edytować (BR-18)", this)
            );
        }
        final SubmissionStatus after;
        if (this == SubmissionStatus.REJECTED) {
            after = SubmissionStatus.SENT;
        } else {
            after = this;
        }
        return after;
    }

    /**
     * Czy zgłoszenie widzi Gość i Odwiedzający.
     *
     * <p>Wymagania: BR-6</p>
     *
     * @return Prawda tylko dla ACCEPTED
     */
    public boolean publiclyVisible() {
        return this == SubmissionStatus.ACCEPTED;
    }
}
