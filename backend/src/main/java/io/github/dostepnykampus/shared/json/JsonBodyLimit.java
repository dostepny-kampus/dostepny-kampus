/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.shared.json;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import org.apache.commons.io.input.BoundedInputStream;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.server.PayloadTooLargeException;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

/**
 * Limit 1 000 000 bajtów ciała żądania wiązanego przez {@code @RequestBody} w całym serwerze.
 * Nagłówek Content-Length ponad limit kończy żądanie przed odczytem; bez nagłówka odczyt
 * zatrzymuje się na pierwszym bajcie ponad limit. W obu przypadkach wyjątek
 * {@link PayloadTooLargeException} daje odpowiedź 413 {@code application/problem+json}.
 *
 * <p>Wymagania: NFR-34</p>
 *
 * @since 0.1.0
 */
@ControllerAdvice
public final class JsonBodyLimit extends RequestBodyAdviceAdapter {

    /**
     * Największy dozwolony rozmiar ciała w bajtach.
     */
    private static final long MAX_BYTES = 1_000_000;

    /**
     * Tworzy limit bez stanu.
     */
    public JsonBodyLimit() {
        super();
    }

    @Override
    public boolean supports(
        final MethodParameter parameter, final Type target,
        final Class<? extends HttpMessageConverter<?>> converter
    ) {
        return true;
    }

    @Override
    public HttpInputMessage beforeBodyRead(
        final HttpInputMessage message, final MethodParameter parameter, final Type target,
        final Class<? extends HttpMessageConverter<?>> converter
    ) {
        if (message.getHeaders().getContentLength() > JsonBodyLimit.MAX_BYTES) {
            throw new PayloadTooLargeException(null);
        }
        return new Bounded(message);
    }

    /**
     * Ciało żądania czytane najwyżej do pierwszego bajtu ponad limit.
     *
     * @since 0.1.0
     */
    private static final class Bounded implements HttpInputMessage {

        /**
         * Oryginalne żądanie.
         */
        private final HttpInputMessage origin;

        Bounded(final HttpInputMessage origin) {
            this.origin = origin;
        }

        @Override
        public InputStream getBody() throws IOException {
            return BoundedInputStream.builder()
                .setInputStream(this.origin.getBody())
                .setMaxCount(JsonBodyLimit.MAX_BYTES + 1)
                .setPropagateClose(false)
                .setOnMaxCount(
                    (max, count) -> {
                        throw new PayloadTooLargeException(null);
                    }
                )
                .get();
        }

        @Override
        public HttpHeaders getHeaders() {
            return this.origin.getHeaders();
        }
    }
}
