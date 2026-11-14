/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.photos

import io.github.dostepnykampus.core.data.submissions.PendingPhotoDraft
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class PhotoContractTest {
    @ParameterizedTest
    @ValueSource(strings = ["", " ", "\n"])
    fun `żądanie wymaga id zdjęcia`(id: String) {
        assertThrows(IllegalArgumentException::class.java) { PhotoRequest(photoId = id) }
    }

    @Test
    fun `wynik nie zamienia żądań`() {
        val request = PhotoRequest(photoId = "pierwsze")
        val other = PendingPhotoDraft(photoId = "drugie", path = "/zdjecia/drugie.jpg")
        assertThrows(IllegalArgumentException::class.java) { PhotoResult.Ready(request = request, photo = other) }
    }
}
