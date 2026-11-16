/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.shared.json;

import io.github.dostepnykampus.shared.error.TooLargeBody;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.input.BoundedInputStream;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;

/**
 * Ciało żądania czytane najwyżej do pierwszego bajtu ponad limit; próba odczytu dalej kończy
 * się wyjątkiem {@link TooLargeBody}. Nagłówki zostają bez zmian.
 *
 * <p>Wymagania: NFR-34</p>
 *
 * @since 0.1.0
 */
final class BoundedBody implements HttpInputMessage {

    /**
     * Oryginalne żądanie.
     */
    private final HttpInputMessage origin;

    /**
     * Największy dozwolony rozmiar ciała w bajtach.
     */
    private final long limit;

    BoundedBody(final HttpInputMessage origin, final long limit) {
        this.origin = origin;
        this.limit = limit;
    }

    @Override
    public InputStream getBody() throws IOException {
        return BoundedInputStream.builder()
            .setInputStream(this.origin.getBody())
            .setMaxCount(this.limit + 1)
            .setPropagateClose(false)
            .setOnMaxCount((max, count) -> BoundedBody.refuse())
            .get();
    }

    @Override
    public HttpHeaders getHeaders() {
        return this.origin.getHeaders();
    }

    private static void refuse() {
        throw new TooLargeBody("Ciało żądania przekracza 1 000 000 bajtów");
    }
}
