/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Stan wysyłki własnego zgłoszenia na liście C3. Wymagania: US8, NFR-5 */
public enum class OwnSubmissionState {
    /** Zapisane w telefonie, czeka na wysłanie („W telefonie”). */
    LOCAL,

    /** Serwer ma treść i wszystkie zdjęcia („Wysłane”). */
    SENT,

    /** Serwer odmówił przyjęcia („Nie wysłano”). */
    REFUSED,
}

/**
 * Własne zgłoszenie Zbierającego na liście „Moje zgłoszenia” (C3).
 *
 * Wiersz zostaje po wysłaniu, gdy kolejka usuwa już swój zapis, więc lista działa bez sieci.
 *
 * Wymagania: US8, NFR-5, BR-9, BR-10, US8.2, BR-16
 *
 * @property id identyfikator UUID zgłoszenia, ten sam co w kolejce i na serwerze
 * @property savedAtMillis chwila ostatniego zapisu w telefonie w milisekundach od epoki Unix
 * @property kind rodzaj zgłoszenia: `PRECISE` albo `ENTRANCE_MARKING`
 * @property buildingSymbol budynek; `null` to „poza budynkiem”
 * @property floorNumber numer piętra budynku podstawowego
 * @property floorId piętro budynku szczegółowego
 * @property barrierType typ bariery TB-1…TB-29
 * @property severity uciążliwość 1–3
 * @property state stan wysyłki
 * @property serverVersion wersja z serwera po wysłaniu; baza dla poprawki z C3
 * @property refusalCode kod odpowiedzi HTTP przy odmowie; `null`, gdy telefon nie odczytał zgłoszenia
 * @property serverStatus status z serwera (`SubmissionStatus`); `null` przed pierwszym odświeżeniem
 * @property rejectionReason powód odrzucenia od Weryfikatora; `null` poza odrzuceniem
 */
@Entity(tableName = "own_submissions")
public data class OwnSubmissionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "saved_at") val savedAtMillis: Long,
    @ColumnInfo(name = "kind") val kind: String,
    @ColumnInfo(name = "building_symbol") val buildingSymbol: String? = null,
    @ColumnInfo(name = "floor_number") val floorNumber: Int? = null,
    @ColumnInfo(name = "floor_id") val floorId: String? = null,
    @ColumnInfo(name = "barrier_type") val barrierType: String? = null,
    @ColumnInfo(name = "severity") val severity: Int? = null,
    @ColumnInfo(name = "state") val state: OwnSubmissionState = OwnSubmissionState.LOCAL,
    @ColumnInfo(name = "server_version") val serverVersion: Int? = null,
    @ColumnInfo(name = "refusal_code") val refusalCode: Int? = null,
    @ColumnInfo(name = "server_status") val serverStatus: String? = null,
    @ColumnInfo(name = "rejection_reason") val rejectionReason: String? = null,
)
