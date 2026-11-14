/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Zgłoszenie zapisane w telefonie, które czeka na wysłanie (status „lokalne”).
 *
 * Pola treści odpowiadają `SubmissionContent` z kontraktu API, a `null` to pole nieobecne.
 *
 * Wymagania: NFR-5, US9.1, BR-9, BR-10, BR-26
 *
 * @property id identyfikator UUID nadany w telefonie; ten sam przy każdej próbie wysłania
 * @property baseVersion wersja z serwera, od której zaczęto edycję; `null` dla nowego zgłoszenia
 * @property savedAtMillis chwila zapisu w telefonie w milisekundach od epoki Unix
 * @property isCreatedOnServer czy serwer ma już zgłoszenie, a czekają tylko zdjęcia
 * @property kind rodzaj zgłoszenia: `PRECISE`, `QUICK` albo `ENTRANCE_MARKING`
 * @property longitude długość geograficzna miejsca w WGS84
 * @property latitude szerokość geograficzna miejsca w WGS84
 * @property accuracyMeters dokładność pozycji w metrach
 * @property isManualCorrection oznaczenie „korekta ręczna”
 * @property buildingSymbol budynek; `null` to „poza budynkiem”
 * @property floorId piętro budynku szczegółowego
 * @property floorNumber numer piętra budynku podstawowego
 * @property isMezzanine dopisek „półpiętro”
 * @property barrierGroup grupa barier
 * @property barrierType typ bariery TB-1…TB-29
 * @property severity uciążliwość 1–3
 * @property description opis
 * @property replacesSubmissionId zgłoszenie przyjęte, które ta zmiana zastąpi
 * @property isBarrierRemoved oznaczenie „bariera usunięta”
 * @property entranceId wejście w zmianie oznaczenia wejścia
 * @property entranceMarking nowe oznaczenie wejścia
 * @property isRefused czy serwer odrzucił zgłoszenie (4xx); takie zgłoszenie czeka w telefonie na decyzję osoby i nie
 *   wraca do wysyłki samo
 */
@Entity(tableName = "pending_submissions")
public data class PendingSubmissionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "base_version") val baseVersion: Int?,
    @ColumnInfo(name = "saved_at") val savedAtMillis: Long,
    @ColumnInfo(name = "created_on_server") val isCreatedOnServer: Boolean,
    val kind: String,
    val longitude: Double,
    val latitude: Double,
    @ColumnInfo(name = "accuracy_meters") val accuracyMeters: Double?,
    @ColumnInfo(name = "manual_correction") val isManualCorrection: Boolean?,
    @ColumnInfo(name = "building_symbol") val buildingSymbol: String?,
    @ColumnInfo(name = "floor_id") val floorId: String?,
    @ColumnInfo(name = "floor_number") val floorNumber: Int?,
    @ColumnInfo(name = "mezzanine") val isMezzanine: Boolean?,
    @ColumnInfo(name = "barrier_group") val barrierGroup: String?,
    @ColumnInfo(name = "barrier_type") val barrierType: String?,
    val severity: Int?,
    val description: String?,
    @ColumnInfo(name = "replaces_submission_id") val replacesSubmissionId: String?,
    @ColumnInfo(name = "barrier_removed") val isBarrierRemoved: Boolean?,
    @ColumnInfo(name = "entrance_id") val entranceId: String?,
    @ColumnInfo(name = "entrance_marking") val entranceMarking: String?,
    @ColumnInfo(name = "refused", defaultValue = "0") val isRefused: Boolean = false,
)

/**
 * Zdjęcie zgłoszenia czekające w telefonie; plik leży w pamięci aplikacji do wysłania całego zgłoszenia.
 *
 * Wymagania: NFR-5
 *
 * @property submissionId zgłoszenie, do którego należy zdjęcie
 * @property photoId identyfikator UUID zdjęcia nadany w telefonie
 * @property path ścieżka pliku JPEG w pamięci aplikacji
 * @property isUploaded czy serwer ma już to zdjęcie
 */
@Entity(
    tableName = "pending_photos",
    primaryKeys = ["submission_id", "photo_id"],
    foreignKeys = [
        ForeignKey(
            entity = PendingSubmissionEntity::class,
            parentColumns = ["id"],
            childColumns = ["submission_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("submission_id")],
)
public data class PendingPhotoEntity(
    @ColumnInfo(name = "submission_id") val submissionId: String,
    @ColumnInfo(name = "photo_id") val photoId: String,
    val path: String,
    @ColumnInfo(name = "uploaded") val isUploaded: Boolean,
)
