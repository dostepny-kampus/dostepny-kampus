/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room.persistence;

import io.github.dostepnykampus.building.room.FloorPlans;
import io.github.dostepnykampus.building.room.PlanFloor;
import io.github.dostepnykampus.building.room.PlanRoom;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Plan budynku w tabelach floors i rooms. Zapis zastępuje piętra i pomieszczenia budynku w jednej
 * transakcji, więc ponowny import tych samych danych daje te same wiersze, bez duplikatów.
 * Piętro spoza nowego planu znika razem z pomieszczeniami. Pomieszczenie z piętra spoza planu
 * jest pomijane, a pusty numer zapisuje się jako NULL. Punkt podpisu leży wewnątrz konturu.
 *
 * <p>TODO(#1824): wywołać zapis z polecenia importu.</p>
 *
 * <p>Wymagania: US16, US18, NFR-40</p>
 *
 * @since 0.1.0
 */
@Component
final class PostgisPlans implements FloorPlans {

    /**
     * Dostęp do tabel floors i rooms.
     */
    private final JdbcClient jdbc;

    /**
     * Transakcja zapisu jednego budynku.
     */
    private final TransactionOperations transaction;

    PostgisPlans(final JdbcClient jdbc, final TransactionOperations transaction) {
        this.jdbc = jdbc;
        this.transaction = transaction;
    }

    @Override
    public void save(
        final String building, final List<PlanFloor> floors, final List<PlanRoom> rooms
    ) {
        final Set<String> known = floors.stream().map(PlanFloor::id).collect(Collectors.toSet());
        this.transaction.executeWithoutResult(
            status -> {
                this.jdbc.sql("DELETE FROM rooms WHERE building_symbol = :building")
                    .param("building", building).update();
                floors.forEach(floor -> this.floor(building, floor));
                this.jdbc.sql(
                    """
                    DELETE FROM floors
                    WHERE building_symbol = :building AND NOT (id = ANY(:ids))
                    """
                ).param("building", building).param("ids", known.toArray(String[]::new))
                    .update();
                rooms.stream().filter(room -> known.contains(room.floorId()))
                    .forEach(room -> this.room(building, room));
            }
        );
    }

    private void floor(final String building, final PlanFloor floor) {
        this.jdbc.sql(
            """
            INSERT INTO floors (building_symbol, id, label, sort_order)
            VALUES (:building, :id, :label, :order)
            ON CONFLICT (building_symbol, id)
            DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order
            """
        ).param("building", building).param("id", floor.id())
            .param("label", floor.label()).param("order", floor.order()).update();
    }

    private void room(final String building, final PlanRoom room) {
        this.jdbc.sql(
            """
            INSERT INTO rooms (
                building_symbol, floor_id, id, number, function, outline, label_point
            )
            SELECT :building, :floor, :id, NULLIF(BTRIM(:number), ''), :function, shape,
                ST_PointOnSurface(shape)
            FROM (SELECT ST_Multi(ST_SetSRID(ST_GeomFromGeoJSON(:outline), 4326)) AS shape) AS room
            """
        ).param("building", building).param("floor", room.floorId()).param("id", room.id())
            .param("number", room.number()).param("function", room.function())
            .param("outline", room.outline()).update();
    }
}
