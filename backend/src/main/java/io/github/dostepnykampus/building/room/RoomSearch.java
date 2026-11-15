/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room;

import java.util.List;

/**
 * Wyszukiwanie sali po numerze w zapisanych planach pięter.
 *
 * <p>Wymagania: US18.1</p>
 *
 * @since 0.1.0
 */
public interface RoomSearch {

    /**
     * Sale we wszystkich budynkach o numerze zaczynającym się od szukanego, bez względu na
     * wielkość liter: jedna na numer i piętro, najpierw dokładne dopasowania numeru, potem budynek
     * i piętro od najniższego.
     *
     * @param number Szukany numer albo jego początek
     * @return Najwyżej 50 sal; pusta lista, gdy nic nie pasuje
     */
    List<RoomHit> rooms(String number);

    /**
     * Sale jednego budynku w tej samej kolejności co {@link #rooms(String)}.
     *
     * @param number Szukany numer albo jego początek
     * @param building Symbol budynku
     * @return Najwyżej 50 sal; pusta lista, gdy nic nie pasuje albo nie ma budynku
     */
    List<RoomHit> rooms(String number, String building);
}
