/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.location

import kotlinx.coroutines.flow.Flow

/** Odczyty pozycji do pomiaru na ekranie C1 (US6.1): w aplikacji [GnssPositionFixes], w testach odczyty ręczne. */
fun interface PositionSource {
    /** Odczyty od teraz, aż do przerwania zbierania. */
    fun fixes(): Flow<PositionFix>
}

/** Stan satelitów do napisu na ekranie C1 (US6.1): w aplikacji [GnssSatellites], w testach stan ręczny. */
fun interface SatelliteSource {
    /** Widoczne satelity po każdej zmianie stanu odbiornika. */
    fun satellites(): Flow<List<Satellite>>
}

/** Włącznik lokalizacji telefonu dla ekranu C1: w aplikacji [GnssLocationSwitch], w testach stan ręczny. */
fun interface LocationSwitch {
    /** `true`, gdy dostawca GPS jest włączony; nowa wartość po każdym przełączeniu lokalizacji w telefonie. */
    fun isEnabled(): Flow<Boolean>
}

/** Zegar monotoniczny telefonu, ten sam co czas odczytów [PositionFix.elapsedMillis]; liczy sekundy pomiaru. */
fun interface ElapsedClock {
    /** Bieżący czas w milisekundach. */
    fun nowMillis(): Long
}
