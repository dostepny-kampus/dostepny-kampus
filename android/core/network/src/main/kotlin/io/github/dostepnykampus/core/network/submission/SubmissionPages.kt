/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network.submission

import io.github.dostepnykampus.core.model.submission.Submission
import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.core.network.decodedResult
import io.github.dostepnykampus.core.network.unexpectedResult
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.Response
import java.net.HttpURLConnection.HTTP_OK

private val NEXT = Regex("""<([^>]*)>\s*;\s*rel="?next"?""")

/**
 * Jedna strona listy zgłoszeń.
 *
 * @property items zgłoszenia strony
 * @property next adres następnej strony; `null` na ostatniej
 */
internal data class SubmissionPage(val items: List<Submission>, val next: HttpUrl?)

/**
 * Cała lista zgłoszeń: kolejne strony spod nagłówka `Link` z `rel="next"` (RFC 8288), aż go zabraknie.
 * Błąd dowolnej strony to błąd całej listy.
 *
 * Wymagania: NFR-34
 *
 * @param target adres strony do pobrania
 * @param collected zgłoszenia poprzednich stron
 * @param page pobiera jedną stronę spod adresu
 * @return zgłoszenia wszystkich stron w kolejności albo błąd pierwszej nieudanej strony
 */
internal tailrec suspend fun allPages(
    target: HttpUrl,
    collected: List<Submission>,
    page: suspend (HttpUrl) -> ApiResult<SubmissionPage>,
): ApiResult<List<Submission>> =
    when (val result = page(target)) {
        is ApiResult.Success -> {
            val items = collected + result.value.items
            val next = result.value.next
            if (next == null || next == target) ApiResult.Success(items) else allPages(next, items, page)
        }

        ApiResult.NotFound -> {
            ApiResult.NotFound
        }

        is ApiResult.NetworkError -> {
            result
        }

        is ApiResult.ServerError -> {
            result
        }
    }

/**
 * Odczytuje stronę listy z odpowiedzi 200: zgłoszenia z treści i adres następnej strony z nagłówka `Link`,
 * rozwiązany względem [target]. Inny kod to wynik spoza kontraktu.
 *
 * Wymagania: NFR-34
 *
 * @param target adres pobranej strony
 * @param json parser JSON kontraktu
 * @return strona albo błąd odpowiedzi
 */
internal fun Response.submissionPage(target: HttpUrl, json: Json): ApiResult<SubmissionPage> =
    if (code == HTTP_OK) {
        decodedResult(code) {
            SubmissionPage(
                items = json.decodeSubmissions(body.string()),
                next =
                    headers("Link").firstNotNullOfOrNull { header ->
                        NEXT.find(header)?.let { target.resolve(it.groupValues[1]) }
                    },
            )
        }
    } else {
        unexpectedResult(code)
    }
