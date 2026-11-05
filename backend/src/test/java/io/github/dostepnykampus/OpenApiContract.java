/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.dialect.Dialects;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.util.function.SingletonSupplier;
import org.springframework.util.function.ThrowingSupplier;
import tools.jackson.databind.JsonNode;

/**
 * Zgodność wymiany HTTP z kontraktem {@code api/openapi.yaml} (decyzja 122).
 *
 * <p>Operację wybiera po metodzie i ścieżce; ścieżka z mniejszą liczbą
 * szablonów ma pierwszeństwo. Odpowiedź wybiera po statusie, potem po
 * zakresie, np. {@code 4XX}, i po typie treści. Operacja, status albo typ
 * treści spoza kontraktu jest naruszeniem. Treść innego typu niż JSON, np.
 * {@code image/jpeg}, jest sprawdzana tylko co do typu. Żądanie niezgodne ze
 * schematem jest dopuszczone, gdy odpowiedź ma status 4xx opisany w kontrakcie.
 * Kontrakt jest wczytywany raz.</p>
 *
 * @since 0.1.0
 */
public final class OpenApiContract implements ResultMatcher {

    /**
     * Adres, pod którym rejestr schematów zna kontrakt.
     */
    private static final String SPEC = "https://dostepny-kampus.test/openapi.yaml";

    /**
     * Kontrakt z repozytorium, wspólny dla wszystkich testów; ścieżka
     * względna od modułu serwera.
     */
    private static final Supplier<Schema> REPOSITORY = OpenApiContract.load(
        () -> Files.readString(Path.of("..", "api", "openapi.yaml"))
    );

    /**
     * Cały kontrakt jako schemat z rejestrem.
     */
    private final Supplier<Schema> spec;

    /**
     * Kontrakt z repozytorium.
     */
    public OpenApiContract() {
        this(OpenApiContract.REPOSITORY);
    }

    /**
     * Kontrakt z tekstu YAML.
     *
     * @param text Tekst kontraktu, czytany najwyżej raz
     */
    public OpenApiContract(final ThrowingSupplier<String> text) {
        this(OpenApiContract.load(text));
    }

    private OpenApiContract(final Supplier<Schema> spec) {
        this.spec = spec;
    }

    @Override
    public void match(final MvcResult result) throws Exception {
        final List<String> found = this.exchange(result);
        if (!found.isEmpty()) {
            throw new AssertionError(String.join(System.lineSeparator(), found));
        }
    }

    /**
     * Naruszenia schematu z {@code components/schemas} kontraktu.
     *
     * @param name Nazwa schematu
     * @param json Treść JSON
     * @return Naruszenia, puste dla zgodnej treści
     */
    public List<String> violations(final String name, final String json) {
        return this.validate("/components/schemas/".concat(name), json);
    }

    private List<String> exchange(final MvcResult result) throws Exception {
        final MockHttpServletRequest request = result.getRequest();
        final MockHttpServletResponse response = result.getResponse();
        final String verb = Objects.toString(request.getMethod(), "");
        final String path = this.template(Objects.toString(request.getRequestURI(), ""));
        final String operation = "/paths/%s/%s".formatted(
            OpenApiContract.escape(path), verb.toLowerCase(Locale.ROOT)
        );
        final String name = "%s %s".formatted(verb, path);
        final List<String> found = new ArrayList<>(0);
        final String sent = new String(
            Objects.requireNonNullElse(request.getContentAsByteArray(), new byte[0]),
            StandardCharsets.UTF_8
        );
        final String reply = this.resolve(this.status(operation, response.getStatus()));
        final String body = response.getContentAsString(StandardCharsets.UTF_8);
        final List<String> asked = new ArrayList<>(0);
        if (!sent.isEmpty()) {
            this.content(operation.concat("/requestBody"), request.getContentType(), sent)
                .forEach(line -> asked.add("%s, żądanie: %s".formatted(name, line)));
        }
        if (this.tree().at(operation).isMissingNode()) {
            found.add("%s: brak operacji w kontrakcie".formatted(name));
        } else if (this.tree().at(reply).isMissingNode()) {
            found.add("%s %d: brak statusu w kontrakcie".formatted(name, response.getStatus()));
        } else if (!body.isEmpty() || this.tree().at(reply).has("content")) {
            this.content(reply, response.getContentType(), body).forEach(
                line -> found.add("%s %d: %s".formatted(name, response.getStatus(), line))
            );
        }
        if (response.getStatus() / 100 != 4) {
            found.addAll(asked);
        }
        return found;
    }

    private List<String> content(
        final String pointer, final @Nullable String type, final String json
    ) {
        String key = "";
        if (type != null) {
            final MediaType media = MediaType.parseMediaType(type);
            key = "%s/%s".formatted(media.getType(), media.getSubtype());
        }
        final String schema = "%s/content/%s/schema".formatted(
            this.resolve(pointer), OpenApiContract.escape(key)
        );
        List<String> found = List.of("typ treści '%s' spoza kontraktu".formatted(key));
        if (!this.tree().at(schema).isMissingNode()) {
            found = List.of();
            if (OpenApiContract.json(type)) {
                found = this.validate(schema, json);
            }
        }
        return found;
    }

    private List<String> validate(final String schema, final String json) {
        return this.spec.get().getSchemaContext().getSchemaRegistry().getSchema(
            SchemaLocation.of("%s#%s".formatted(OpenApiContract.SPEC, schema))
        ).validate(json, InputFormat.JSON).stream().map(
            error -> "%s %s".formatted(error.getInstanceLocation(), error.getMessage())
        ).toList();
    }

    private String status(final String operation, final int code) {
        String key = String.valueOf(code);
        if (this.tree().at(operation).path("responses").path(key).isMissingNode()) {
            key = "%dXX".formatted(code / 100);
        }
        return "%s/responses/%s".formatted(operation, key);
    }

    private String resolve(final String pointer) {
        String target = pointer;
        while (this.tree().at(target).has("$ref")) {
            target = this.tree().at(target).get("$ref").asString().substring(1);
        }
        return target;
    }

    private String template(final String uri) {
        return this.tree().path("paths").propertyNames().stream().filter(
            candidate -> Pattern.compile(
                Pattern.quote(candidate).replaceAll("\\{[^/]+}", "\\\\E[^/]+\\\\Q")
            ).matcher(uri).matches()
        ).min(Comparator.comparingInt(candidate -> candidate.split("\\{").length)).orElse(uri);
    }

    private JsonNode tree() {
        return this.spec.get().getSchemaNode();
    }

    private static Supplier<Schema> load(final ThrowingSupplier<String> text) {
        return SingletonSupplier.of(
            () -> SchemaRegistry.withDialect(
                Dialects.getOpenApi31(),
                builder -> builder.schemas(Map.of(OpenApiContract.SPEC, text.get()))
            ).getSchema(SchemaLocation.of(OpenApiContract.SPEC))
        );
    }

    private static boolean json(final @Nullable String type) {
        String subtype = "";
        if (type != null) {
            subtype = MediaType.parseMediaType(type).getSubtype();
        }
        return "json".equals(subtype) || subtype.endsWith("+json");
    }

    private static String escape(final String segment) {
        return segment.replace("~", "~0").replace("/", "~1");
    }
}
