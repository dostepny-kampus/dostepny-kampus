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

/** Historia własnych zgłoszeń w telefonie dla listy C3 (US8, NFR-5). */
@Dao
interface OwnSubmissionDao {
    /** Własne zgłoszenia od najnowszego; zmienia się po każdym zapisie i zmianie stanu. */
    @Query("SELECT * FROM own_submissions ORDER BY saved_at DESC, id")
    fun observe(): Flow<List<OwnSubmissionEntity>>

    /**
     * Zapisuje zgłoszenie; ten sam identyfikator zastępuje poprzedni zapis, więc poprawka nie robi duplikatu.
     *
     * @param submission zgłoszenie
     */
    @Upsert
    suspend fun save(submission: OwnSubmissionEntity)

    /**
     * Zgłoszenie [id] z historii; `null`, gdy go nie ma.
     *
     * @param id identyfikator zgłoszenia
     */
    @Query("SELECT * FROM own_submissions WHERE id = :id")
    suspend fun find(id: String): OwnSubmissionEntity?

    /**
     * Zapamiętuje wersję z serwera po przyjęciu treści; baza dla poprawki z C3 (BR-10).
     *
     * @param id identyfikator zgłoszenia
     * @param version wersja z serwera
     */
    @Query("UPDATE own_submissions SET server_version = :version WHERE id = :id")
    suspend fun saveVersion(id: String, version: Int)

    /**
     * Oznacza zgłoszenie jako wysłane: serwer ma treść i wszystkie zdjęcia.
     *
     * @param id identyfikator zgłoszenia
     */
    @Query("UPDATE own_submissions SET state = 'SENT', refusal_code = NULL WHERE id = :id")
    suspend fun markSent(id: String)

    /**
     * Oznacza odmowę serwera z kodem odpowiedzi.
     *
     * @param id identyfikator zgłoszenia
     * @param code kod odpowiedzi HTTP; `null`, gdy serwer nie podał kodu
     */
    @Query("UPDATE own_submissions SET state = 'REFUSED', refusal_code = :code WHERE id = :id")
    suspend fun markRefused(id: String, code: Int?)

    /**
     * Zapisuje status z serwera po odświeżeniu listy C3; stan wysyłki z telefonu zostaje (US8.2, BR-16).
     *
     * @param id identyfikator zgłoszenia
     * @param status status z serwera
     * @param reason powód odrzucenia; `null` poza odrzuceniem
     * @param version wersja z serwera; baza dla poprawki (BR-10)
     */
    @Query(
        "UPDATE own_submissions SET server_status = :status, rejection_reason = :reason, server_version = :version " +
            "WHERE id = :id",
    )
    suspend fun saveServerStatus(id: String, status: String, reason: String?, version: Int)
}
