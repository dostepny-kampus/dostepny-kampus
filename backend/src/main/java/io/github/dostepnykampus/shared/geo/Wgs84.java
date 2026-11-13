/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.shared.geo;


/**
 * Zakres współrzędnych WGS84, jedna definicja dla podpowiedzi budynku
 * i punktu zgłoszenia: wartość skończona w domkniętym zakresie osi.
 *
 * @since 0.1.0
 */
public enum Wgs84 {

    /**
     * Szerokość geograficzna, od -90 do 90 stopni.
     */
    LATITUDE(90.0),

    /**
     * Długość geograficzna, od -180 do 180 stopni.
     */
    LONGITUDE(180.0);

    /**
     * Największa wartość bezwzględna osi w stopniach.
     */
    private final double limit;

    Wgs84(final double limit) {
        this.limit = limit;
    }

    /**
     * Czy współrzędna mieści się w zakresie osi; NaN i nieskończoność nie.
     *
     * @param value Współrzędna w stopniach
     * @return Prawda dla wartości skończonej w domkniętym zakresie
     */
    public boolean holds(final double value) {
        return Math.abs(value) <= this.limit;
    }
}
