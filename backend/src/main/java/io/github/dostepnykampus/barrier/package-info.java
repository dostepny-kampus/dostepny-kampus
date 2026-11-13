/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

/**
 * Moduł barrier: zgłoszenia barier i ich zdjęcia.
 *
 * <p>Obsługuje zgłoszenie bariery przez Odwiedzającego, formularz
 * zgłoszenia Zbierającego, status zgłoszeń autora i podgląd
 * podobnych zgłoszeń dla Weryfikatora. Zapis zdjęcia blokuje wiersz zgłoszenia,
 * aby przyjęcie nie wyprzedziło zapisu obiektu do magazynu.</p>
 *
 * <p>Wymagania: US4, US7, US8, US10.1, BR-13</p>
 *
 * @since 0.1.0
 */
@NullMarked
package io.github.dostepnykampus.barrier;

import org.jspecify.annotations.NullMarked;
