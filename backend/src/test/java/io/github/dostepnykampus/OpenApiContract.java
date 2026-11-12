/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.mockmvc.MockMvcRequest;
import com.atlassian.oai.validator.mockmvc.MockMvcResponse;
import com.atlassian.oai.validator.report.LevelResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.dialect.Dialects;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.util.function.SingletonSupplier;
import org.springframework.util.function.ThrowingSupplier;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Zgodność wymiany HTTP z kontraktem {@code api/openapi.yaml} (decyzja 122).
 *
 * <p>Wymianę sprawdza biblioteka {@code openapi-request-validator} (#830):
 * operację, parametry ścieżki, zapytania i nagłówków, treść żądania, status,
 * typ treści i treść odpowiedzi. Operacja, status albo typ treści spoza
 * kontraktu jest naruszeniem. Żądanie niezgodne z kontraktem jest dopuszczone,
 * gdy odpowiedź ma status 4xx opisany w kontrakcie. Tożsamość testy dają
 * przez filtry zabezpieczeń, nie nagłówkiem, więc biblioteka nie sprawdza
 * {@code security}. Biblioteka nie dopisuje też {@code additionalProperties:
 * false}, bo kontrakt zamyka obiekty przez {@code unevaluatedProperties}.
 * Schematy treści dla testów JSON daje rejestr schematów. Kontrakt jest
 * wczytywany raz.</p>
 *
 * @since 0.1.0
 */
public final class OpenApiContract implements ResultMatcher {

    /**
     * Adres, pod którym rejestr schematów zna kontrakt.
     */
    private static final String SPEC = "https://dostepny-kampus.test/openapi.yaml";

    /**
     * Tekst kontraktu z repozytorium, wspólny dla wszystkich testów; ścieżka
     * względna od modułu serwera.
     */
    private static final Supplier<String> REPOSITORY = SingletonSupplier.of(
        ThrowingSupplier.of(OpenApiContract::text)
    );

    /**
     * Kontrakt z repozytorium jako schemat z rejestrem.
     */
    private static final Supplier<Schema> REPOSITORY_SPEC = OpenApiContract.load(
        OpenApiContract.REPOSITORY
    );

    /**
     * Cały kontrakt jako schemat z rejestrem.
     */
    private final Supplier<Schema> spec;

    /**
     * Drzewo kontraktu w Jackson 3; rejestr schematów działa na Jackson 2.
     */
    private final Supplier<JsonNode> tree;

    /**
     * Walidator wymiany HTTP z biblioteki (#830).
     */
    private final Supplier<OpenApiInteractionValidator> validator;

    /**
     * Kontrakt z repozytorium.
     */
    public OpenApiContract() {
        this(OpenApiContract.REPOSITORY_SPEC, OpenApiContract.REPOSITORY);
    }

    /**
     * Kontrakt z tekstu YAML.
     *
     * @param text Tekst kontraktu, czytany najwyżej raz
     */
    public OpenApiContract(final ThrowingSupplier<String> text) {
        this(SingletonSupplier.of(text));
    }

    private OpenApiContract(final Supplier<String> text) {
        this(OpenApiContract.load(text), text);
    }

    private OpenApiContract(final Supplier<Schema> spec, final Supplier<String> text) {
        this(spec, OpenApiContract.jackson(spec), OpenApiContract.interactions(text));
    }

    private OpenApiContract(
        final Supplier<Schema> spec, final Supplier<JsonNode> tree,
        final Supplier<OpenApiInteractionValidator> validator
    ) {
        this.spec = spec;
        this.tree = tree;
        this.validator = validator;
    }

    @Override
    public void match(final MvcResult result) {
        final MockHttpServletRequest request = result.getRequest();
        final int status = result.getResponse().getStatus();
        final String name = "%s %s %d".formatted(
            request.getMethod(), request.getRequestURI(), status
        );
        final List<String> found = this.validator.get().validate(
            MockMvcRequest.of(request), MockMvcResponse.of(result.getResponse())
        ).getMessages().stream().filter(
            message -> status / 100 != 4 || !OpenApiContract.asked(message.getKey())
        ).map(message -> "%s: %s".formatted(name, OpenApiContract.describe(message))).toList();
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

    /**
     * Wartości wyliczenia w schemacie lub w jego polu.
     *
     * @param pointer Względna ścieżka od components/schemas
     * @return Wartości JSON w kolejności z kontraktu
     */
    public List<JsonNode> enumValues(final String pointer) {
        final JsonNode values = this.tree().at(
            this.resolve("/components/schemas/".concat(pointer)).concat("/enum")
        );
        if (!values.isArray()) {
            throw new AssertionError("Brak wyliczenia OpenAPI dla ".concat(pointer));
        }
        return StreamSupport.stream(values.spliterator(), false).toList();
    }

    /**
     * Tekst kontraktu z repozytorium; jedyne miejsce testów, które czyta plik (#895).
     *
     * @return Tekst YAML kontraktu, ścieżka względna od modułu serwera
     * @throws IOException Gdy pliku nie da się przeczytać
     */
    static String text() throws IOException {
        return Files.readString(Path.of("..", "api", "openapi.yaml"));
    }

    /**
     * Operacje kontraktu w kolejności z pliku: metoda HTTP pod każdą ścieżką, z prefiksem
     * serwera i wpisem {@code security} operacji albo globalnym.
     *
     * @return Operacje kontraktu
     */
    List<ContractOperation> operations() {
        final JsonNode root = this.tree();
        final String prefix = ContractOperation.prefix(root);
        final List<ContractOperation> found = new ArrayList<>(0);
        for (final Map.Entry<String, JsonNode> path : root.path("paths").properties()) {
            for (final Map.Entry<String, JsonNode> verb : path.getValue().properties()) {
                if (ContractOperation.verb(verb.getKey())) {
                    JsonNode security = verb.getValue().path("security");
                    if (security.isMissingNode()) {
                        security = root.path("security");
                    }
                    found.add(
                        new ContractOperation(
                            ContractOperation.name(verb.getKey(), prefix, path.getKey()), security
                        )
                    );
                }
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

    private String resolve(final String pointer) {
        String target = pointer;
        while (this.tree().at(target).has("$ref")) {
            target = this.tree().at(target).get("$ref").asString().substring(1);
        }
        return target;
    }

    private JsonNode tree() {
        return this.tree.get();
    }

    private static Supplier<Schema> load(final Supplier<String> text) {
        return SingletonSupplier.of(
            () -> SchemaRegistry.withDialect(
                Dialects.getOpenApi31(),
                builder -> builder.schemas(Map.of(OpenApiContract.SPEC, text.get()))
            ).getSchema(SchemaLocation.of(OpenApiContract.SPEC))
        );
    }

    private static Supplier<JsonNode> jackson(final Supplier<Schema> spec) {
        return SingletonSupplier.of(
            () -> JsonMapper.shared().readTree(spec.get().getSchemaNode().toString())
        );
    }

    private static Supplier<OpenApiInteractionValidator> interactions(
        final Supplier<String> text
    ) {
        return SingletonSupplier.of(
            () -> OpenApiInteractionValidator.createForInlineApiSpecification(text.get())
                .withLevelResolver(OpenApiContract.levels()).build()
        );
    }

    private static LevelResolver levels() {
        return LevelResolver.create().withLevel(
            "validation.request.security", ValidationReport.Level.IGNORE
        ).withLevel(
            "validation.schema.additionalProperties", ValidationReport.Level.IGNORE
        ).build();
    }

    private static String describe(final ValidationReport.Message message) {
        return "%s %s %s".formatted(
            message.getKey(),
            message.getContext().flatMap(ValidationReport.MessageContext::getPointers)
                .map(ValidationReport.MessageContext.Pointers::getInstance).orElse(""),
            message.getMessage()
        );
    }

    private static boolean asked(final String key) {
        return key.startsWith("validation.request.")
            && !"validation.request.path.missing".equals(key)
            && !"validation.request.operation.notAllowed".equals(key);
    }
}
