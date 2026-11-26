/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.review.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Zasłanianie twarzy i tablic na zdjęciu, bez paska nawigacji; wejście z R2 „Zasłoń twarz lub tablicę”.
 *
 * Wymagania: BR-15, NFR-25
 *
 * @property submissionId identyfikator sprawdzanego zgłoszenia
 * @property photoId identyfikator zasłanianego zdjęcia
 * @property photoUrl adres zdjęcia na R2; po zapisie obraz spod tego adresu trzeba wczytać od nowa
 */
@Serializable
data class ReviewCoverRoute(val submissionId: String, val photoId: String, val photoUrl: String) : NavKey
