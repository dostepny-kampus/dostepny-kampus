/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.inspection.domain;

import java.util.UUID;

/**
 * Port zapisu i cofania znaczników piętra obejrzanego.
 *
 * <p>Wymagania: US15.1, BR-9, BR-20</p>
 *
 * @since 0.1.0
 */
public interface MarkerRepository {

    /**
     * Zapisuje znacznik; znacznik o zapisanym już identyfikatorze zostaje bez zmian.
     *
     * @param marker Znacznik do zapisu
     */
    void place(NewMarker marker);

    /**
     * Cofa własny znacznik Zbierającego; cofnięcie już cofniętego zostawia go cofniętym.
     *
     * <p>Wymagania: US15.2, BR-20</p>
     *
     * @param id Identyfikator cofanego znacznika
     * @param author Identyfikator konta, które postawiło znacznik
     * @return Prawda, gdy konto ma taki znacznik; fałsz dla nieistniejącego albo cudzego
     */
    boolean revoke(UUID id, String author);
}
