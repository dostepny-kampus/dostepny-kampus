/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.database.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

/** Odczyt i zapis kopii planów pięter i ich pomieszczeń. Wymagania: US16.1, NFR-10 */
@Dao
public interface FloorPlanDao {
    /** Piętra budynku [building] od najniższego; pusta lista, gdy telefon nie ma jego planu. */
    @Query("SELECT * FROM floor_plans WHERE building_symbol = :building ORDER BY sort_order")
    public suspend fun floors(building: String): List<FloorPlanEntity>

    /** Plan piętra [floor] budynku [building] albo `null`, gdy telefon go nie ma. */
    @Query("SELECT * FROM floor_plans WHERE building_symbol = :building AND floor_id = :floor")
    public suspend fun plan(building: String, floor: String): FloorPlanEntity?

    /** Pomieszczenia piętra [floor] budynku [building] w kolejności identyfikatorów. */
    @Query("SELECT * FROM floor_rooms WHERE building_symbol = :building AND floor_id = :floor ORDER BY id")
    public suspend fun rooms(building: String, floor: String): List<FloorRoomEntity>

    /**
     * Zastępuje cały plan budynku [building] planami [plans] i pomieszczeniami [rooms] wszystkich pięter w jednej
     * transakcji: piętro, którego serwer już nie podaje, znika z kopii.
     */
    @Transaction
    public suspend fun replace(building: String, plans: List<FloorPlanEntity>, rooms: List<FloorRoomEntity>) {
        deleteRooms(building)
        deletePlans(building)
        insertPlans(plans)
        insertRooms(rooms)
    }

    /** Usuwa pomieszczenia budynku [building]. */
    @Query("DELETE FROM floor_rooms WHERE building_symbol = :building")
    public suspend fun deleteRooms(building: String)

    /** Usuwa plany pięter budynku [building]. */
    @Query("DELETE FROM floor_plans WHERE building_symbol = :building")
    public suspend fun deletePlans(building: String)

    /** Dopisuje plany pięter. */
    @Insert
    public suspend fun insertPlans(plans: List<FloorPlanEntity>)

    /** Dopisuje pomieszczenia. */
    @Insert
    public suspend fun insertRooms(rooms: List<FloorRoomEntity>)
}
