/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Historia własnych zgłoszeń w telefonie dla listy C3. Wymagania: US8, NFR-5 */
@Dao
public interface OwnSubmissionDao {
    /** Własne zgłoszenia od najnowszego; zmienia się po każdym zapisie i zmianie stanu. */
    @Query("SELECT * FROM own_submissions ORDER BY saved_at DESC, id")
    public fun observe(): Flow<List<OwnSubmissionEntity>>

    /**
     * Zapisuje zgłoszenie; ten sam identyfikator zastępuje poprzedni zapis, więc poprawka nie robi duplikatu.
     *
     * @param submission zgłoszenie
     */
    @Upsert
    public suspend fun save(submission: OwnSubmissionEntity)

    /**
     * Zgłoszenie [id] z historii; `null`, gdy go nie ma.
     *
     * @param id identyfikator zgłoszenia
     */
    @Query("SELECT * FROM own_submissions WHERE id = :id")
    public suspend fun find(id: String): OwnSubmissionEntity?

    /**
     * Zapamiętuje wersję z serwera po przyjęciu treści; baza dla poprawki z C3.
     *
     * Wymagania: BR-10
     *
     * @param id identyfikator zgłoszenia
     * @param version wersja z serwera
     */
    @Query("UPDATE own_submissions SET server_version = :version WHERE id = :id")
    public suspend fun saveVersion(id: String, version: Int)

    /**
     * Oznacza zgłoszenie jako wysłane: serwer ma treść i wszystkie zdjęcia.
     *
     * @param id identyfikator zgłoszenia
     */
    @Query("UPDATE own_submissions SET state = 'SENT', refusal_code = NULL WHERE id = :id")
    public suspend fun markSent(id: String)

    /**
     * Oznacza odmowę serwera z kodem odpowiedzi.
     *
     * @param id identyfikator zgłoszenia
     * @param code kod odpowiedzi HTTP; `null`, gdy telefon nie odczytał zgłoszenia (brak pliku zdjęcia, zły wiersz)
     */
    @Query("UPDATE own_submissions SET state = 'REFUSED', refusal_code = :code WHERE id = :id")
    public suspend fun markRefused(id: String, code: Int?)

    /**
     * Zapisuje status z serwera po odświeżeniu listy C3; stan wysyłki z telefonu zostaje.
     *
     * Wymagania: US8.2, BR-16, BR-10
     *
     * @param id identyfikator zgłoszenia
     * @param status status z serwera
     * @param reason powód odrzucenia; `null` poza odrzuceniem
     * @param version wersja z serwera; baza dla poprawki
     */
    @Query(
        "UPDATE own_submissions SET server_status = :status, rejection_reason = :reason, server_version = :version " +
            "WHERE id = :id",
    )
    public suspend fun saveServerStatus(id: String, status: String, reason: String?, version: Int)
}
