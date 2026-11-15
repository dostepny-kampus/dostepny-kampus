/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.persistence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.migration.JavaMigration;
import org.flywaydb.core.api.resolver.MigrationResolver;
import org.flywaydb.core.api.resolver.ResolvedMigration;
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;

/**
 * Udostępnia migrację importu tylko wskazanej lokalizacji migracji.
 *
 * @since 0.1.0
 */
final class OsmMigrationResolver implements FlywayConfigurationCustomizer, MigrationResolver {

    /**
     * Powtarzalna migracja importu.
     */
    private final JavaMigration migration;

    /**
     * Lokalizacja migracji, której historia wykonuje import.
     */
    private final String location;

    OsmMigrationResolver(final JavaMigration migration, final String location) {
        this.migration = migration;
        this.location = location;
    }

    @Override
    public void customize(final FluentConfiguration configuration) {
        configuration.javaMigrations(
            Arrays.stream(configuration.getJavaMigrations())
                .filter(existing -> !this.migration.equals(existing))
                .toArray(JavaMigration[]::new)
        );
        final MigrationResolver[] resolvers = configuration.getResolvers();
        final MigrationResolver[] registered = Arrays.copyOf(resolvers, resolvers.length + 1);
        registered[resolvers.length] = this;
        configuration.resolvers(registered);
    }

    @Override
    public Collection<ResolvedMigration> resolveMigrations(
        final MigrationResolver.Context context
    ) {
        final List<ResolvedMigration> found = new ArrayList<>(1);
        if (Arrays.stream(context.configuration.getLocations())
            .anyMatch(item -> this.location.equals(item.getPath()))) {
            found.add(
                this.migration.getResolvedMigration(
                    context.configuration, context.statementInterceptor
                )
            );
        }
        return found;
    }
}
