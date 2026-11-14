/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

package io.github.dostepnykampus.building.room.persistence;

import io.github.dostepnykampus.building.room.FloorPlans;
import io.github.dostepnykampus.building.room.PlanFloor;
import io.github.dostepnykampus.building.room.PlanRoom;
import java.sql.Types;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Zapis pięter i pomieszczeń do tabel PostgreSQL bez duplikatów.
 *
 * @since 0.1.0
 */
@Component
public final class JdbcFloorPlans implements FloorPlans {

    /** Klient zapytań JDBC. */
    private final JdbcClient jdbc;

    /** Szablon transakcji bazy danych. */
    private final TransactionTemplate transaction;

    /**
     * Konstruktor.
     *
     * @param client Klient zapytań JDBC
     * @param template Szablon transakcji
     */
    public JdbcFloorPlans(final JdbcClient client, final TransactionTemplate template) {
        this.jdbc = client;
        this.transaction = template;
    }

    @Override
    public void save(
        final String building, final List<PlanFloor> floors, final List<PlanRoom> rooms
    ) {
        this.transaction.executeWithoutResult(
            status -> {
                final Set<String> savedfloors = new HashSet<>(floors.size());
                for (final PlanFloor floor : floors) {
                    this.jdbc.sql(
                        """
                        INSERT INTO floors (building_symbol, id, label, sort_order)
                        VALUES (?, ?, ?, ?) ON CONFLICT (building_symbol, id) DO UPDATE
                        SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order
                        """
                    ).params(building, floor.id(), floor.label(), floor.order()).update();
                    savedfloors.add(floor.id());
                }
                this.deleteStale(building, savedfloors, this.upsertRooms(building, rooms));
            }
        );
    }

    private Set<Map.Entry<String, String>> upsertRooms(
        final String building, final List<PlanRoom> rooms
    ) {
        final Map<String, Integer> counts = new HashMap<>(rooms.size());
        final Set<Map.Entry<String, String>> kept = new HashSet<>(rooms.size());
        for (final PlanRoom room : rooms) {
            final String raw = room.number();
            final String num;
            final Object sqlnum;
            if (raw == null || raw.isBlank()) {
                num = "";
                sqlnum = new SqlParameterValue(Types.VARCHAR, null);
            } else {
                num = raw.strip();
                sqlnum = num;
            }
            final String group = "%d:%s:%d:%s:%s".formatted(
                room.floorId().length(), room.floorId(), num.length(), num, room.function()
            );
            final int ord = counts.getOrDefault(group, 0) + 1;
            counts.put(group, ord);
            final String id = "%d:%s:%d:%s".formatted(num.length(), num, ord, room.function());
            this.jdbc.sql(
                """
                INSERT INTO rooms (building_symbol, floor_id, id, number, function, outline)
                VALUES (?, ?, ?, ?, ?, ST_Multi(ST_SetSRID(ST_GeomFromGeoJSON(?), 4326)))
                ON CONFLICT (building_symbol, floor_id, id) DO UPDATE
                SET number=EXCLUDED.number, function=EXCLUDED.function, outline=EXCLUDED.outline
                """
            ).params(building, room.floorId(), id, sqlnum, room.function(), room.outline())
                .update();
            kept.add(Map.entry(room.floorId(), id));
        }
        return kept;
    }

    private void deleteStale(
        final String building, final Set<String> floors, final Set<Map.Entry<String, String>> rooms
    ) {
        final List<Map.Entry<String, String>> current = this.jdbc
            .sql("SELECT floor_id, id FROM rooms WHERE building_symbol = ?").param(building)
            .query((rows, idx) -> Map.entry(rows.getString(1), rows.getString(2))).list();
        for (final Map.Entry<String, String> entry : current) {
            if (!rooms.contains(entry)) {
                this.jdbc.sql(
                    "DELETE FROM rooms WHERE building_symbol = ? AND floor_id = ? AND id = ?"
                ).params(building, entry.getKey(), entry.getValue()).update();
            }
        }
        final List<String> existing = this.jdbc
            .sql("SELECT id FROM floors WHERE building_symbol = ?").param(building)
            .query(String.class).list();
        for (final String floor : existing) {
            if (!floors.contains(floor)) {
                this.jdbc.sql("DELETE FROM floors WHERE building_symbol = ? AND id = ?")
                    .params(building, floor).update();
            }
        }
    }
}
