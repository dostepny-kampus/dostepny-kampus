/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.inspection.json;

import io.github.dostepnykampus.shared.error.InvalidInput;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Odczyt jednej operacji paczki osobno od pozostałych: błąd typu pola, znak NUL albo brak
 * wymaganego pola daje operację z błędem, a nie odrzucenie paczki. Brak poprawnego
 * clientOperationId kończy się wyjątkiem InvalidInput, bo bez niego nie ma komu dać wyniku.
 *
 * <p>Wymagania: US9.2, BR-9</p>
 *
 * @since 0.1.0
 */
@Component
public final class SyncOperationReader {

    /**
     * Opis błędu danych operacji bez szczegółów wewnętrznych.
     */
    private static final String INVALID = "Pole operacji ma niepoprawną wartość";

    /**
     * Wiązanie operacji z tymi samymi kontrolami co ciało żądania.
     */
    private final JsonMapper mapper;

    /**
     * Tworzy odczyt operacji.
     *
     * @param mapper Wiązanie JSON z tymi samymi kontrolami co ciało żądania
     */
    public SyncOperationReader(final JsonMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Czyta operację i sprawdza jej kształt.
     *
     * @param node Operacja z paczki
     * @param author Identyfikator konta z tokenu
     * @return Operacja poprawna albo z opisem błędu
     */
    public ReadOperation read(final JsonNode node, final String author) {
        final UUID id = SyncOperationReader.id(node);
        ReadOperation result;
        try {
            final SyncOperationJson operation = this.mapper.treeToValue(
                node, SyncOperationJson.class
            );
            operation.check(author);
            result = new ReadOperation.Valid(operation);
        } catch (final InvalidInput ex) {
            result = new ReadOperation.Broken(
                id, Objects.requireNonNullElse(ex.getMessage(), SyncOperationReader.INVALID)
            );
        } catch (final JacksonException ex) {
            result = new ReadOperation.Broken(id, SyncOperationReader.INVALID);
        }
        return result;
    }

    private static UUID id(final JsonNode node) {
        try {
            return UUID.fromString(node.path("clientOperationId").asString());
        } catch (final IllegalArgumentException ex) {
            throw new InvalidInput("Brak identyfikatora operacji w polu clientOperationId", ex);
        }
    }
}
