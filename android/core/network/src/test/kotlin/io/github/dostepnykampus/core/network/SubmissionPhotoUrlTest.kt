/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import io.github.dostepnykampus.core.network.di.NetworkModule
import io.github.dostepnykampus.core.network.submission.SubmissionsApi
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Adres zdjęcia zgłoszenia przyjętego dla biblioteki obrazów według `api/openapi.yaml`. Wymagania: US2.2, BR-17 */
class SubmissionPhotoUrlTest {
    /** Adres to serwer, `/submissions/{submissionId}/photos/{photoId}` i nic więcej. */
    @Test
    fun buildsPhotoAddress() {
        val api = SubmissionsApi(OkHttpClient(), NetworkModule.json(), "http://10.0.2.2:8080/")
        val url = api.photoUrl(submissionId = "6f1c", photoId = "9a2b")
        assertEquals("http://10.0.2.2:8080/submissions/6f1c/photos/9a2b", url, "adres zdjęcia")
    }

    /** Adres serwera ze ścieżką zostaje przed `/submissions`. */
    @Test
    fun keepsServerPath() {
        val api = SubmissionsApi(OkHttpClient(), NetworkModule.json(), "https://example.org/api/")
        val url = api.photoUrl(submissionId = "6f1c", photoId = "9a2b")
        assertEquals("https://example.org/api/submissions/6f1c/photos/9a2b", url, "adres ze ścieżką serwera")
    }

    /** Ukośnik i znak zapytania w identyfikatorze nie zmieniają zasobu ani nie tworzą zapytania. */
    @Test
    fun encodesPathSegments() {
        val api = SubmissionsApi(OkHttpClient(), NetworkModule.json(), "http://10.0.2.2:8080/")
        val url = api.photoUrl(submissionId = "a/b", photoId = "c?d")
        assertEquals("http://10.0.2.2:8080/submissions/a%2Fb/photos/c%3Fd", url, "zakodowane segmenty")
    }
}
