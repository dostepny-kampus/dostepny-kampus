/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.identity.web;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import java.time.Duration;

/**
 * Kubełki tokenów Bucket4j według klucza (adres albo konto), odnawiane co minutę do limitu.
 * Kubełki kluczy nieaktywnych dłużej niż dwie minuty znikają z pamięci (Caffeine).
 *
 * <p>Wymagania: NFR-34</p>
 *
 * @since 0.1.0
 */
final class MinuteBuckets {

    /**
     * Liczba żądań na minutę dla jednego klucza.
     */
    private final int limit;

    /**
     * Kubełki według klucza.
     */
    private final Cache<String, Bucket> buckets;

    MinuteBuckets(final int limit) {
        this(
            limit,
            Caffeine.newBuilder().expireAfterAccess(Duration.ofMinutes(2))
                .maximumSize(100_000).build()
        );
    }

    private MinuteBuckets(final int limit, final Cache<String, Bucket> buckets) {
        this.limit = limit;
        this.buckets = buckets;
    }

    /**
     * Zabiera jeden token z kubełka klucza.
     *
     * @param key Adres klienta albo identyfikator konta
     * @return Wynik próby z czasem do odnowienia, gdy tokenów brakuje
     */
    ConsumptionProbe take(final String key) {
        return this.buckets.get(key, any -> this.bucket()).tryConsumeAndReturnRemaining(1);
    }

    private Bucket bucket() {
        return Bucket.builder().addLimit(
            Bandwidth.builder().capacity(this.limit)
                .refillIntervally(this.limit, Duration.ofMinutes(1)).build()
        ).build();
    }
}
